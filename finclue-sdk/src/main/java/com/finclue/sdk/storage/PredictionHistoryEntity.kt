package com.finclue.sdk.storage

import androidx.room.Entity

/** A host-authorized, locally stored candidate selection used only for personal ranking. */
@Entity(
    tableName = "prediction_history",
    primaryKeys = ["contextKey", "normalizedText"],
)
internal data class PredictionHistoryEntity(
    val contextKey: String,
    val normalizedText: String,
    val displayText: String,
    val selectionCount: Int,
    val lastSelectedAtEpochMillis: Long,
)

