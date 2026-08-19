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
        val generated = withContext(Dispatchers.Default) {
            LlamaNativeRuntime.generateCandidates(
                conditioningText = predictionContext.conditioningText(),
                prefix = prefix,
                candidateCount = PARALLEL_CANDIDATE_COUNT,
                maxTokens = predictionContext.maxGeneratedTokens,
                seedOffset = 0,
            )
        }
        generated.asSequence()
            .flatMap { GeneratedCandidateValidator.validate(predictionContext, prefix, it) }
            .forEach(accepted::add)
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
            append("<|im_start|>system\n")
            append("당신은 한국어 자동완성 엔진입니다. assistant 응답의 시작 부분은 이미 입력되어 있습니다. ")
            append("입력된 부분을 반복하거나 설명하지 말고, 바로 뒤에 이어질 글자만 생성합니다.")
            append("<|im_end|>\n<|im_start|>user\n")
            append("입력 유형: ").append(typeLabel).append('\n')
            append("현재 입력을 자연스럽게 완성하세요.")
            append("<|im_end|>\n<|im_start|>assistant\n")
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
        const val PARALLEL_CANDIDATE_COUNT = 6
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
