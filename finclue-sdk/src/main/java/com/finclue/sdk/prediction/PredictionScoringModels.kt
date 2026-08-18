package com.finclue.sdk.prediction

internal data class LmCandidate(
    val text: String,
    /** Average suffix log probability. Higher values are better. */
    val logProbability: Float,
)

internal data class MergedCandidate(
    val text: String,
    val normalizedText: String,
    val lmLogProbability: Float?,
    val selectionCount: Int,
    val lastSelectedAtEpochMillis: Long?,
)

