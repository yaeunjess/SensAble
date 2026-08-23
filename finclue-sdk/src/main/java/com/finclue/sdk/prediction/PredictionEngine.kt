package com.finclue.sdk.prediction

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.finclue.sdk.api.FieldSpec
import com.finclue.sdk.api.PredictionCandidate
import com.finclue.sdk.api.PredictionRequest
import com.finclue.sdk.api.PredictionSelection
import com.finclue.sdk.api.PredictionMode
import com.finclue.sdk.api.HistoryPolicy
import com.finclue.sdk.api.PredictionContext

internal class PredictionEngine(context: Context) {
    private val historyStore = PredictionHistoryStore(context)
    private val scorer = LlamaCandidateScorer(context)
    private val ranker = PersonalRanker()
    private val resultCache = PredictionResultCache(MAX_CACHE_ENTRIES)

    suspend fun prewarm() {
        val startedAt = SystemClock.elapsedRealtime()
        scorer.prewarm()
        Log.i(TAG, "prediction prewarm completed in ${SystemClock.elapsedRealtime() - startedAt} ms")
    }

    suspend fun suggest(field: FieldSpec, prefix: String, limit: Int = 3): List<PredictionCandidate> {
        if (field.predictionMode == PredictionMode.DISABLED || prefix.isBlank() || limit <= 0) {
            return emptyList()
        }
        return suggest(
            predictionContext = field.predictionContext,
            prefix = prefix,
            mode = field.predictionMode,
            historyPolicy = field.historyPolicy,
            limit = limit,
        )
    }

    suspend fun suggest(request: PredictionRequest): List<PredictionCandidate> = suggest(
        predictionContext = request.context,
        prefix = request.currentText,
        mode = request.mode,
        historyPolicy = request.historyPolicy,
        limit = request.limit,
    )

    suspend fun suggestPersonal(request: PredictionRequest): List<PredictionCandidate> {
        if (request.mode != PredictionMode.PERSONALIZED || request.currentText.isBlank()) {
            return emptyList()
        }
        val personal = historyStore.findByPrefix(
            context = request.context,
            prefix = request.currentText,
            policy = request.historyPolicy,
            limit = PERSONAL_CANDIDATE_LIMIT,
        )
        return ranker.rank(CandidateMerger.merge(emptyList(), personal), request.limit)
    }

    private suspend fun suggest(
        predictionContext: PredictionContext,
        prefix: String,
        mode: PredictionMode,
        historyPolicy: HistoryPolicy,
        limit: Int,
    ): List<PredictionCandidate> {
        if (mode == PredictionMode.DISABLED || prefix.isBlank() || limit <= 0) return emptyList()
        val cacheKey = PredictionCacheKey(
            context = predictionContext,
            normalizedPrefix = PredictionHistoryStore.normalize(prefix),
            mode = mode,
            historyPolicy = historyPolicy,
            limit = limit,
        )
        resultCache[cacheKey]?.let { return it }

        val personal = if (mode == PredictionMode.PERSONALIZED) {
            historyStore.findByPrefix(
                context = predictionContext,
                prefix = prefix,
                policy = historyPolicy,
                limit = PERSONAL_CANDIDATE_LIMIT,
            )
        } else {
            emptyList()
        }

        if (personal.size >= limit) {
            return ranker.rank(
                CandidateMerger.merge(emptyList(), personal),
                limit,
            ).also { resultCache[cacheKey] = it }
        }

        val generationStartedAt = SystemClock.elapsedRealtime()
        val generated = scorer.generate(
            predictionContext,
            prefix,
            limit.coerceAtMost(GENERATED_CANDIDATE_LIMIT),
        )
        val generationMillis = SystemClock.elapsedRealtime() - generationStartedAt
        val generatedInModelOrder = generated.mapIndexed { index, text ->
            LmCandidate(text = text, logProbability = -index.toFloat())
        }
        Log.i(
            TAG,
            "prediction timing context=${predictionContext.storageKey} " +
                "generated=${generated.size} generationMs=$generationMillis " +
                "rescoring=disabled",
        )
        return ranker.rank(CandidateMerger.merge(generatedInModelOrder, personal), limit)
            .also { resultCache[cacheKey] = it }
    }

    suspend fun recordSelection(field: FieldSpec, selectedText: String) {
        if (field.predictionMode != PredictionMode.PERSONALIZED) return
        historyStore.recordSelection(field.predictionContext, selectedText, field.historyPolicy)
        if (field.historyPolicy == HistoryPolicy.READ_WRITE) {
            resultCache.invalidate(field.predictionContext)
        }
    }

    suspend fun recordSelection(selection: PredictionSelection) {
        historyStore.recordSelection(selection.context, selection.text, selection.historyPolicy)
        if (selection.historyPolicy == HistoryPolicy.READ_WRITE) {
            resultCache.invalidate(selection.context)
        }
    }

    fun clearCache() = resultCache.clear()

    suspend fun close() = scorer.close()

    private companion object {
        const val TAG = "FincluePrediction"
        const val GENERATED_CANDIDATE_LIMIT = 6
        const val PERSONAL_CANDIDATE_LIMIT = 16
        const val MAX_CACHE_ENTRIES = 64
    }
}

internal data class PredictionCacheKey(
    val context: PredictionContext,
    val normalizedPrefix: String,
    val mode: PredictionMode,
    val historyPolicy: HistoryPolicy,
    val limit: Int,
)

internal class PredictionResultCache(private val maxEntries: Int) {
    init {
        require(maxEntries > 0)
    }

    private val values = object : LinkedHashMap<PredictionCacheKey, List<PredictionCandidate>>(
        maxEntries,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<PredictionCacheKey, List<PredictionCandidate>>,
        ): Boolean = size > maxEntries
    }

    operator fun get(key: PredictionCacheKey): List<PredictionCandidate>? =
        synchronized(values) { values[key] }

    operator fun set(key: PredictionCacheKey, candidates: List<PredictionCandidate>) {
        synchronized(values) { values[key] = candidates.toList() }
    }

    fun invalidate(context: PredictionContext) {
        synchronized(values) {
            values.keys.removeAll { it.context == context }
        }
    }

    fun clear() = synchronized(values) { values.clear() }
}
