from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

app = FastAPI()

model_path = "../toxic_model"
tokenizer = AutoTokenizer.from_pretrained(model_path)
model = AutoModelForSequenceClassification.from_pretrained(model_path)

class TextRequest(BaseModel):
    text: str

@app.post("/predict")
def predict(req: TextRequest):
    inputs = tokenizer(req.text, return_tensors="pt", truncation=True, padding=True)
    outputs = model(**inputs)
    probs = torch.nn.functional.softmax(outputs.logits, dim=-1)
    label = torch.argmax(probs, dim=-1).item() # 0 = safe, 1 = toxic
    return {"label": label, "probabilities": probs.tolist()}