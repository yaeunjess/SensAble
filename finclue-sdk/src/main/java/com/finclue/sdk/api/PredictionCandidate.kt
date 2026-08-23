package com.finclue.sdk.api

/** One final autocomplete candidate returned in descending score order. */
data class PredictionCandidate(
    val text: String,
    val score: Float,
)

