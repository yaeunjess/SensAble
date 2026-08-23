package com.finclue.sdk.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flow_sessions")
internal data class FlowSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long? = null,
    val status: String = STATUS_STARTED,
) {
    companion object {
        const val STATUS_STARTED = "STARTED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_CANCELLED = "CANCELLED"
        const val STATUS_ERROR = "ERROR"
    }
}
