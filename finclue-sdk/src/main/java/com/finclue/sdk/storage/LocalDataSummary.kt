package com.finclue.sdk.storage

data class LocalDataSummary(
    val totalSessions: Int,
    val completedSessions: Int,
    val cancelledSessions: Int,
    val errorSessions: Int,
    val recordedFields: Int,
    val averageCompletedDurationMillis: Long?,
    val lastUsedAtEpochMillis: Long?,
)
