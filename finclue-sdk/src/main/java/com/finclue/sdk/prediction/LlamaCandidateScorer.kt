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
                conditioningText = predictionContext.conditioningText(prefix),
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
                conditioningText = predictionContext.conditioningText(prefix),
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

    private fun PredictionContext.conditioningText(prefix: String): String {
        val profile = when (this) {
            PredictionContext.GENERAL -> PromptProfile(
                role = "자연스러운 한국어 문장을 이어 쓰는 자동완성기입니다. assistant 응답에는 현재 입력이 이미 적혀 있습니다. 입력을 반복하거나 설명하지 말고 문장의 나머지만 이어서 생성합니다.",
                examplePrefix = "안녕",
                exampleCompletion = "안녕하세요.",
                request = "자연스러운 일반 한국어 문장으로 완성하세요.",
            )
            PredictionContext.BANK_NAME -> PromptProfile(
                role = "대한민국 은행 이름 자동완성기입니다. assistant 응답에는 현재 입력이 이미 적혀 있습니다. 조사나 설명 없이 은행 이름의 남은 글자만 이어서 생성합니다.",
                examplePrefix = "신한",
                exampleCompletion = "신한은행",
                request = "대한민국 은행 이름 하나로 완성하세요.",
            )
            PredictionContext.PERSON_NAME -> PromptProfile(
                role = "한국 사람 이름 자동완성기입니다. assistant 응답에는 현재 입력이 이미 적혀 있습니다. 오직 이름을 완성할 한글 한두 글자만 이어서 생성합니다. 조사나 문장을 만들지 않습니다.",
                examplePrefix = "김민",
                exampleCompletion = "김민준",
                request = "2~4글자의 한국 사람 이름으로 완성하세요.",
            )
            PredictionContext.ORGANIZATION_NAME -> PromptProfile(
                role = "대한민국 기관과 단체 이름 자동완성기입니다. assistant 응답에는 현재 입력이 이미 적혀 있습니다. 설명 없이 기관명의 남은 부분만 이어서 생성합니다.",
                examplePrefix = "한국관광",
                exampleCompletion = "한국관광공사",
                request = "대한민국 기관 또는 단체 이름 하나로 완성하세요.",
            )
            PredictionContext.ADDRESS -> PromptProfile(
                role = "대한민국 주소 자동완성기입니다. assistant 응답에는 현재 입력이 이미 적혀 있습니다. 설명이나 문장 없이 주소의 남은 부분만 이어서 생성합니다.",
                examplePrefix = "부산 해운대",
                exampleCompletion = "부산 해운대구",
                request = "대한민국 주소 하나로 자연스럽게 완성하세요.",
            )
            PredictionContext.PRODUCT_NAME -> PromptProfile(
                role = "한국어 금융 상품 이름 자동완성기입니다. assistant 응답에는 현재 입력이 이미 적혀 있습니다. 설명 없이 상품명의 남은 부분만 이어서 생성합니다.",
                examplePrefix = "청년",
                exampleCompletion = "청년적금",
                request = "금융 상품 이름 하나로 완성하세요.",
            )
        }
        return buildString {
            append("<|im_start|>system\n")
            append(profile.role)
            append("<|im_end|>\n<|im_start|>user\n")
            append("현재 입력: ").append(profile.examplePrefix)
            append("<|im_end|>\n<|im_start|>assistant\n")
            append(profile.exampleCompletion)
            append("<|im_end|>\n<|im_start|>user\n")
            append("현재 입력: ").append(prefix).append('\n')
            append(profile.request)
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

private data class PromptProfile(
    val role: String,
    val examplePrefix: String,
    val exampleCompletion: String,
    val request: String,
)

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
