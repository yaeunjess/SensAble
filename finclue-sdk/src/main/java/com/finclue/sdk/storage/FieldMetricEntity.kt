package com.finclue.sdk.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Privacy-preserving field telemetry. Raw field values and braille cells are never stored.
 */
@Entity(
    tableName = "field_metrics",
    foreignKeys = [
        ForeignKey(
            entity = FlowSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
internal data class FieldMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val fieldKey: String,
    val fieldType: String,
    val inputLength: Int,
    val correctionCount: Int,
    val durationMillis: Long,
)
