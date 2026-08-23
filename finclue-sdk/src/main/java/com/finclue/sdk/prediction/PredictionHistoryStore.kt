package com.finclue.sdk.prediction

import android.content.Context
import com.finclue.sdk.api.HistoryPolicy
import com.finclue.sdk.api.PredictionContext
import com.finclue.sdk.storage.FinclueDatabase
import com.finclue.sdk.storage.PredictionHistoryEntity
import java.text.Normalizer

/** Applies the host-selected history policy at the persistence boundary. */
internal class PredictionHistoryStore(context: Context) {
    private val dao = FinclueDatabase.getInstance(context).finclueDao()

    suspend fun findByPrefix(
        context: PredictionContext,
        prefix: String,
        policy: HistoryPolicy,
        limit: Int,
    ): List<PersonalCandidate> {
        if (policy == HistoryPolicy.NONE || limit <= 0) return emptyList()
        val normalizedPrefix = normalize(prefix)
        if (normalizedPrefix.isEmpty()) return emptyList()

        return dao.findPredictionHistory(
            contextKey = context.storageKey,
            normalizedPrefix = normalizedPrefix,
            limit = limit,
        ).map {
            PersonalCandidate(
                text = it.displayText,
                selectionCount = it.selectionCount,
                lastSelectedAtEpochMillis = it.lastSelectedAtEpochMillis,
            )
        }
    }

    suspend fun recordSelection(
        context: PredictionContext,
        text: String,
        policy: HistoryPolicy,
        selectedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        if (policy != HistoryPolicy.READ_WRITE) return
        val displayText = Normalizer.normalize(text.trim(), Normalizer.Form.NFC)
        val normalizedText = normalize(displayText)
        if (normalizedText.isEmpty()) return

        dao.recordPredictionSelection(
            PredictionHistoryEntity(
                contextKey = context.storageKey,
                normalizedText = normalizedText,
                displayText = displayText,
                selectionCount = 1,
                lastSelectedAtEpochMillis = selectedAtEpochMillis,
            )
        )
    }

    internal companion object {
        fun normalize(value: String): String =
            Normalizer.normalize(value.trim(), Normalizer.Form.NFC).lowercase()
    }
}

