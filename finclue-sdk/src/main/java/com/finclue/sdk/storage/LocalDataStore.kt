package com.finclue.sdk.storage

import android.content.Context

internal class LocalDataStore(context: Context) {
    private val dao = FinclueDatabase.getInstance(context).finclueDao()

    suspend fun startSession(now: Long = System.currentTimeMillis()): Long =
        dao.insertSession(FlowSessionEntity(startedAtEpochMillis = now))

    suspend fun finishSession(sessionId: Long, status: String, now: Long = System.currentTimeMillis()) {
        dao.finishSession(sessionId, status, now)
    }

    suspend fun recordField(
        sessionId: Long,
        fieldKey: String,
        fieldType: String,
        inputLength: Int,
        correctionCount: Int,
        durationMillis: Long,
    ) {
        dao.insertFieldMetric(
            FieldMetricEntity(
                sessionId = sessionId,
                fieldKey = fieldKey,
                fieldType = fieldType,
                inputLength = inputLength,
                correctionCount = correctionCount,
                durationMillis = durationMillis,
            )
        )
    }

    suspend fun summary() = LocalDataSummary(
        totalSessions = dao.totalSessionCount(),
        completedSessions = dao.completedSessionCount(),
        cancelledSessions = dao.cancelledSessionCount(),
        errorSessions = dao.errorSessionCount(),
        recordedFields = dao.fieldMetricCount(),
        averageCompletedDurationMillis = dao.averageCompletedDurationMillis()?.toLong(),
        lastUsedAtEpochMillis = dao.lastUsedAtEpochMillis(),
    )

    suspend fun clear() = dao.clearAllSessions()
}
