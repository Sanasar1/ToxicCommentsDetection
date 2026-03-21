package com.example.toxicity.dto

data class ToxicityPredictionResponse(
    val label: Int,
    val probs: List<Float>?
)
