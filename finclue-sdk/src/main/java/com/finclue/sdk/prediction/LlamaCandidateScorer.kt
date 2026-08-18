package com.finclue.sdk.prediction

import android.content.Context
import com.finclue.sdk.api.PredictionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class LlamaCandidateScorer(context: Context) {
    private val installer = BundledModelInstaller(context)
    private val mutex = Mutex()
    private var loaded = false

    suspend fun score(
        predictionContext: PredictionContext,
        prefix: String,
        candidates: List<String>,
    ): List<LmCandidate> = mutex.withLock {
        val validCandidates = candidates.filter { it.startsWith(prefix) && it != prefix }
        if (prefix.isBlank() || validCandidates.isEmpty()) return@withLock emptyList()
        ensureLoaded()

        val scores = withContext(Dispatchers.Default) {
            LlamaNativeRuntime.scoreCandidates(
                conditioningText = predictionContext.conditioningText(),
                candidates = validCandidates.toTypedArray(),
            )
        }
        check(scores.size == validCandidates.size) { "Native scorer returned an invalid result." }
        validCandidates.mapIndexed { index, text -> LmCandidate(text, scores[index]) }
    }

    suspend fun close() = mutex.withLock {
        if (loaded) {
            LlamaNativeRuntime.unloadModel()
            loaded = false
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        val model = installer.installIfNeeded()
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
        withContext(Dispatchers.Default) {
            LlamaNativeRuntime.loadModel(model.absolutePath, CONTEXT_SIZE, threads)
        }
        loaded = true
    }

    private fun PredictionContext.conditioningText(): String = when (this) {
        PredictionContext.GENERAL -> "입력 유형: 일반 텍스트\n자동완성: "
        PredictionContext.BANK_NAME -> "입력 유형: 은행명\n자동완성: "
        PredictionContext.PERSON_NAME -> "입력 유형: 사람 이름\n자동완성: "
        PredictionContext.ORGANIZATION_NAME -> "입력 유형: 기관명\n자동완성: "
        PredictionContext.ADDRESS -> "입력 유형: 주소\n자동완성: "
        PredictionContext.PRODUCT_NAME -> "입력 유형: 상품명\n자동완성: "
    }

    private companion object {
        const val CONTEXT_SIZE = 256
    }
}
