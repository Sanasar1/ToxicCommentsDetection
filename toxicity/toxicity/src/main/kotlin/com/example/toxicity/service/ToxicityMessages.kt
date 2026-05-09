package com.example.toxicity.service

private val CATEGORY_LABELS = mapOf(
    "obvious_toxicity" to "obvious toxicity",
)

private const val MAX_CATEGORIES_IN_MESSAGE = 3

internal fun buildToxicMessage(subject: String, rejectedBy: List<String>): String {
    val pretty = rejectedBy
        .take(MAX_CATEGORIES_IN_MESSAGE)
        .map { CATEGORY_LABELS[it] ?: it.replace('_', ' ') }
    return when (pretty.size) {
        0 -> "$subject rejected: content classified as toxic"
        1 -> "$subject rejected: content classified as ${pretty[0]}"
        else -> "$subject rejected: content classified as ${pretty.joinToString(", ")}"
    }
}
