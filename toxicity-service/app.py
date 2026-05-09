from __future__ import annotations

import logging
import os
import re
from typing import Dict, Optional

import torch
from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoModelForSequenceClassification, AutoTokenizer


_CYRILLIC_RE = re.compile(r"[Ѐ-ӿԀ-ԯ]")
_LATIN_RE = re.compile(r"[A-Za-z]")


def _detect_lang(text: str) -> str:
    if not text:
        return "en"
    cyr = len(_CYRILLIC_RE.findall(text))
    lat = len(_LATIN_RE.findall(text))
    if cyr == 0 and lat == 0:
        return "en"
    return "ru" if cyr >= lat else "en"


logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
log = logging.getLogger("toxicity-service")

MODELS_ROOT = os.environ.get("MODELS_ROOT", "/models")
LEGACY_MODEL_PATH = os.environ.get("LEGACY_MODEL_PATH", "../toxic_model")

DETECTOR_NAMES = ("obvious_toxicity",)

ENGLISH_ONLY_DETECTORS = frozenset()


def _threshold(name: str, default: float = 0.5) -> float:
    raw = (
        os.environ.get(f"{name.upper()}_THRESHOLD")
        or os.environ.get("DETECTOR_THRESHOLD")
        or str(default)
    )
    try:
        v = float(raw)
        if not 0.0 < v < 1.0:
            raise ValueError(v)
        return v
    except ValueError:
        log.warning("invalid threshold for %s: %r, falling back to %s", name, raw, default)
        return default


THRESHOLDS: Dict[str, float] = {n: _threshold(n) for n in DETECTOR_NAMES}


class _Detector:
    def __init__(self, name: str, path: str):
        self.name = name
        self.path = path
        self.tokenizer = AutoTokenizer.from_pretrained(path)
        self.model = AutoModelForSequenceClassification.from_pretrained(path)
        self.model.eval()

    @torch.inference_mode()
    def predict(self, text: str) -> Dict[str, float]:
        inputs = self.tokenizer(
            text,
            return_tensors="pt",
            truncation=True,
            padding=True,
            max_length=256,
        )
        logits = self.model(**inputs).logits
        probs = torch.nn.functional.softmax(logits, dim=-1)[0].tolist()
        label = int(torch.argmax(logits, dim=-1).item())
        return {"label": label, "prob_toxic": float(probs[1]), "prob_safe": float(probs[0])}


def _resolve_path(name: str) -> Optional[str]:
    candidates = [
        os.path.join(MODELS_ROOT, name, "model"),
        os.path.join(MODELS_ROOT, name),
    ]
    if name == "obvious_toxicity":
        candidates.append(LEGACY_MODEL_PATH)
    for p in candidates:
        if p and os.path.isdir(p) and os.path.exists(os.path.join(p, "config.json")):
            return p
    return None


def _load_all() -> Dict[str, _Detector]:
    loaded: Dict[str, _Detector] = {}
    for name in DETECTOR_NAMES:
        path = _resolve_path(name)
        if path is None:
            log.warning("detector %s: no model directory found, skipping", name)
            continue
        try:
            log.info("detector %s: loading from %s", name, path)
            loaded[name] = _Detector(name, path)
            log.info("detector %s: ready", name)
        except Exception as e:
            log.exception("detector %s: failed to load (%s); skipping", name, e)
    if not loaded:
        log.error("no detectors loaded; /predict will refuse all content")
    return loaded


detectors: Dict[str, _Detector] = _load_all()


app = FastAPI(title="Toxicity service", version="2.0")


class TextRequest(BaseModel):
    text: str


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "loaded": list(detectors.keys()),
        "expected": list(DETECTOR_NAMES),
        "thresholds": THRESHOLDS,
        "english_only": sorted(ENGLISH_ONLY_DETECTORS),
    }


@app.post("/predict")
def predict(req: TextRequest) -> dict:
    details: Dict[str, dict] = {}
    rejected_by: list[str] = []

    if not detectors:
        return {
            "label": 1,
            "rejected_by": ["__no_model_loaded__"],
            "details": {
                name: {"available": False, "reason": "model not loaded"}
                for name in DETECTOR_NAMES
            },
        }

    detected_lang = _detect_lang(req.text)
    for name in DETECTOR_NAMES:
        det = detectors.get(name)
        if det is None:
            details[name] = {"available": False, "reason": "model directory not found"}
            continue
        if name in ENGLISH_ONLY_DETECTORS and detected_lang != "en":
            details[name] = {
                "available": True, "skipped": True,
                "reason": f"english-only detector skipped on {detected_lang!r} text",
            }
            continue
        try:
            r = det.predict(req.text)
            threshold = THRESHOLDS[name]
            flagged = r["prob_toxic"] >= threshold
            details[name] = {"available": True, "threshold": threshold, "flagged": flagged, **r}
            if flagged:
                rejected_by.append(name)
        except Exception as e:
            log.exception("detector %s: prediction failed", name)
            details[name] = {"available": False, "reason": f"prediction error: {e}"}

    label = 1 if rejected_by else 0
    return {"label": label, "rejected_by": rejected_by, "details": details}
