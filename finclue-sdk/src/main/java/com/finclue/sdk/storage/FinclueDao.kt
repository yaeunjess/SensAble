package com.finclue.sdk.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

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
}
