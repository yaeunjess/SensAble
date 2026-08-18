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
        val accepted = linkedSetOf<String>()
        suspend fun generateBatch(candidateCount: Int, seedOffset: Int) {
            val generated = withContext(Dispatchers.Default) {
                LlamaNativeRuntime.generateCandidates(
                    conditioningText = predictionContext.conditioningText(),
                    prefix = prefix,
                    candidateCount = candidateCount,
                    maxTokens = predictionContext.maxGeneratedTokens,
                    seedOffset = seedOffset,
                )
            }
            generated.asSequence()
                .flatMap { GeneratedCandidateValidator.validate(predictionContext, prefix, it) }
                .forEach(accepted::add)
        }

        val initialAttempts = count.coerceAtMost(INITIAL_GENERATION_ATTEMPTS)
        generateBatch(initialAttempts, seedOffset = 0)
        if (accepted.size < count && initialAttempts < MAX_GENERATION_ATTEMPTS) {
            generateBatch(
                candidateCount = MAX_GENERATION_ATTEMPTS - initialAttempts,
                seedOffset = initialAttempts,
            )
        }
        accepted.take(count)
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

    private fun PredictionContext.conditioningText(): String {
        val typeLabel = when (this) {
            PredictionContext.GENERAL -> "일반 텍스트"
            PredictionContext.BANK_NAME -> "은행명"
            PredictionContext.PERSON_NAME -> "한국 사람 이름"
            PredictionContext.ORGANIZATION_NAME -> "기관명"
            PredictionContext.ADDRESS -> "대한민국 주소"
            PredictionContext.PRODUCT_NAME -> "상품명"
        }
        return buildString {
            append("[|system|]\n")
            append("당신은 한국어 자동완성 엔진입니다. 설명 없이 요청한 유형의 완성 결과 하나만 출력합니다.")
            append("[|endofturn|]\n[|user|]\n")
            append("입력 유형: ").append(typeLabel).append('\n')
            append("사용자가 입력한 글자로 시작하는 자연스러운 완성 결과 하나를 출력하세요.")
            append("[|endofturn|]\n[|assistant|]\n<think>\n\n</think>\n\n")
        }
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
        const val INITIAL_GENERATION_ATTEMPTS = 3
        const val MAX_GENERATION_ATTEMPTS = 6
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
