package com.finclue.sdk.prediction

import android.content.Context
import com.finclue.sdk.api.FieldSpec
import com.finclue.sdk.api.PredictionCandidate
import com.finclue.sdk.api.PredictionRequest
import com.finclue.sdk.api.PredictionSelection
import com.finclue.sdk.api.PredictionMode

internal class PredictionEngine(context: Context) {
    private val candidateProvider = AssetGeneralCandidateProvider(context)
    private val historyStore = PredictionHistoryStore(context)
    private val scorer = LlamaCandidateScorer(context)
    private val ranker = PersonalRanker()

    suspend fun suggest(field: FieldSpec, prefix: String, limit: Int = 3): List<PredictionCandidate> {
        if (field.predictionMode == PredictionMode.DISABLED || prefix.isBlank() || limit <= 0) {
            return emptyList()
        }
        val general = candidateProvider.findByPrefix(prefix, GENERAL_CANDIDATE_LIMIT)
        val scored = scorer.score(field.predictionContext, prefix, general)
        val personal = if (field.predictionMode == PredictionMode.PERSONALIZED) {
            historyStore.findByPrefix(
                context = field.predictionContext,
                prefix = prefix,
                policy = field.historyPolicy,
                limit = PERSONAL_CANDIDATE_LIMIT,
            )
        } else {
            emptyList()
        }
        return ranker.rank(CandidateMerger.merge(scored, personal), limit)
    }

    suspend fun suggest(request: PredictionRequest): List<PredictionCandidate> {
        val general = candidateProvider.findByPrefix(request.currentText, GENERAL_CANDIDATE_LIMIT)
        val scored = scorer.score(request.context, request.currentText, general)
        val personal = if (request.mode == PredictionMode.PERSONALIZED) {
            historyStore.findByPrefix(
                context = request.context,
                prefix = request.currentText,
                policy = request.historyPolicy,
                limit = PERSONAL_CANDIDATE_LIMIT,
            )
        } else {
            emptyList()
        }
        return ranker.rank(CandidateMerger.merge(scored, personal), request.limit)
    }

    suspend fun recordSelection(field: FieldSpec, selectedText: String) {
        if (field.predictionMode != PredictionMode.PERSONALIZED) return
        historyStore.recordSelection(field.predictionContext, selectedText, field.historyPolicy)
    }

    suspend fun recordSelection(selection: PredictionSelection) {
        historyStore.recordSelection(selection.context, selection.text, selection.historyPolicy)
    }

    suspend fun close() = scorer.close()

    private companion object {
        const val GENERAL_CANDIDATE_LIMIT = 16
        const val PERSONAL_CANDIDATE_LIMIT = 16
    }
}
