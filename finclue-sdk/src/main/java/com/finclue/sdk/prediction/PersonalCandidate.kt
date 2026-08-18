package com.finclue.sdk.prediction

internal data class PersonalCandidate(
    val text: String,
    val selectionCount: Int,
    val lastSelectedAtEpochMillis: Long,
)

