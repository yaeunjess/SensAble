package com.finclue.sdk.prediction

import android.content.Context
import com.finclue.sdk.api.PredictionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.Normalizer

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

    suspend fun generate(
        predictionContext: PredictionContext,
        prefix: String,
        count: Int,
    ): List<String> = mutex.withLock {
        if (prefix.isBlank() || count <= 0) return@withLock emptyList()
        ensureLoaded()
        val generated = withContext(Dispatchers.Default) {
            LlamaNativeRuntime.generateCandidates(
                conditioningText = predictionContext.conditioningText(),
                prefix = prefix,
                candidateCount = count,
                maxTokens = predictionContext.maxGeneratedTokens,
            )
        }
        generated.asSequence()
            .flatMap { GeneratedCandidateValidator.validate(predictionContext, prefix, it) }
            .distinct()
            .take(count)
            .toList()
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

    private val PredictionContext.maxGeneratedTokens: Int
        get() = if (this == PredictionContext.PERSON_NAME) {
            MAX_PERSON_NAME_GENERATED_TOKENS
        } else {
            MAX_GENERAL_GENERATED_TOKENS
        }

    private companion object {
        const val CONTEXT_SIZE = 256
        const val MAX_PERSON_NAME_GENERATED_TOKENS = 6
        const val MAX_GENERAL_GENERATED_TOKENS = 8
    }
}

internal object GeneratedCandidateValidator {
    private val personNameSeparator = Regex("[,;:/|\\r\\n]+")
    private val hangulName = Regex("[가-힣]+")

    fun validate(
        context: PredictionContext,
        prefix: String,
        value: String,
    ): Sequence<String> {
        val normalizedPrefix = Normalizer.normalize(prefix.trim(), Normalizer.Form.NFC)
        if (context == PredictionContext.PERSON_NAME) {
            if (normalizedPrefix.length >= MAX_PERSON_NAME_CHARACTERS ||
                normalizedPrefix.any { it !in '\uAC00'..'\uD7A3' }
            ) {
                return emptySequence()
            }
            val normalizedValue = Normalizer.normalize(value, Normalizer.Form.NFC)
            return normalizedValue.splitToSequence(personNameSeparator)
                .map { it.trim().trim('.', '!', '?', '。') }
                .filter { candidate ->
                    candidate.startsWith(normalizedPrefix) &&
                        candidate != normalizedPrefix &&
                        candidate.length in MIN_PERSON_NAME_CHARACTERS..MAX_PERSON_NAME_CHARACTERS &&
                        hangulName.matches(candidate)
                }
        }

        val firstSegment = value.lineSequence().firstOrNull().orEmpty()
            .substringBefore(',').substringBefore(';').substringBefore(':')
            .trim().trimEnd('.', '!', '?', '。')
        val candidate = Normalizer.normalize(firstSegment, Normalizer.Form.NFC)
        if (candidate == normalizedPrefix || !candidate.startsWith(normalizedPrefix)) return emptySequence()
        if (candidate.any(Char::isISOControl)) return emptySequence()
        return sequenceOf(candidate).filter { it.length <= MAX_GENERAL_CHARACTERS }
    }

    private const val MAX_GENERAL_CHARACTERS = 30
    private const val MIN_PERSON_NAME_CHARACTERS = 2
    private const val MAX_PERSON_NAME_CHARACTERS = 4
}
