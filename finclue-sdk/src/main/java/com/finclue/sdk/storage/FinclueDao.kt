package com.finclue.sdk.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
internal interface FinclueDao {
    @Insert
    suspend fun insertSession(session: FlowSessionEntity): Long

    @Query(
        """
        UPDATE flow_sessions
        SET status = :status, finishedAtEpochMillis = :finishedAt
        WHERE id = :sessionId
        """
    )
    suspend fun finishSession(sessionId: Long, status: String, finishedAt: Long)

    @Insert
    suspend fun insertFieldMetric(metric: FieldMetricEntity)

    @Query("SELECT COUNT(*) FROM flow_sessions")
    suspend fun totalSessionCount(): Int

    @Query("SELECT COUNT(*) FROM flow_sessions WHERE status = 'COMPLETED'")
    suspend fun completedSessionCount(): Int

    @Query("SELECT COUNT(*) FROM flow_sessions WHERE status = 'CANCELLED'")
    suspend fun cancelledSessionCount(): Int

    @Query("SELECT COUNT(*) FROM flow_sessions WHERE status = 'ERROR'")
    suspend fun errorSessionCount(): Int

    @Query("SELECT COUNT(*) FROM field_metrics")
    suspend fun fieldMetricCount(): Int

    @Query("SELECT MAX(startedAtEpochMillis) FROM flow_sessions")
    suspend fun lastUsedAtEpochMillis(): Long?

    @Query(
        """
        SELECT AVG(finishedAtEpochMillis - startedAtEpochMillis)
        FROM flow_sessions
        WHERE status = 'COMPLETED' AND finishedAtEpochMillis IS NOT NULL
        """
    )
    suspend fun averageCompletedDurationMillis(): Double?

    @Query("DELETE FROM flow_sessions")
    suspend fun clearAllSessions()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPredictionHistoryIfAbsent(history: PredictionHistoryEntity): Long

    @Query(
        """
        UPDATE prediction_history
        SET selectionCount = selectionCount + 1,
            displayText = :displayText,
            lastSelectedAtEpochMillis = :selectedAt
        WHERE contextKey = :contextKey AND normalizedText = :normalizedText
        """
    )
    suspend fun incrementPredictionHistory(
        contextKey: String,
        normalizedText: String,
        displayText: String,
        selectedAt: Long,
    )

    @Transaction
    suspend fun recordPredictionSelection(history: PredictionHistoryEntity) {
        val inserted = insertPredictionHistoryIfAbsent(history)
        if (inserted == -1L) {
            incrementPredictionHistory(
                contextKey = history.contextKey,
                normalizedText = history.normalizedText,
                displayText = history.displayText,
                selectedAt = history.lastSelectedAtEpochMillis,
            )
        }
    }

    @Query(
        """
        SELECT * FROM prediction_history
        WHERE contextKey = :contextKey
          AND substr(normalizedText, 1, length(:normalizedPrefix)) = :normalizedPrefix
        ORDER BY selectionCount DESC, lastSelectedAtEpochMillis DESC
        LIMIT :limit
        """
    )
    suspend fun findPredictionHistory(
        contextKey: String,
        normalizedPrefix: String,
        limit: Int,
    ): List<PredictionHistoryEntity>

    @Query("DELETE FROM prediction_history")
    suspend fun clearPredictionHistory()

    @Transaction
    suspend fun clearAllLocalData() {
        clearAllSessions()
        clearPredictionHistory()
    }
}
