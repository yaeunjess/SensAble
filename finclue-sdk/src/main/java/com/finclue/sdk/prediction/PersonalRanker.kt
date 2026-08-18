package com.finclue.sdk.prediction

import com.finclue.sdk.api.PredictionCandidate
import kotlin.math.exp
import kotlin.math.pow

internal data class RankingConfig(
    val lmWeight: Float = 0.60f,
    val frequencyWeight: Float = 0.25f,
    val recencyWeight: Float = 0.15f,
    val recencyHalfLifeDays: Float = 30f,
    val missingLmScore: Float = 0.5f,
) {
    init {
        require(lmWeight >= 0f && frequencyWeight >= 0f && recencyWeight >= 0f)
        require(lmWeight + frequencyWeight + recencyWeight > 0f)
        require(recencyHalfLifeDays > 0f)
        require(missingLmScore in 0f..1f)
    }
}

internal class PersonalRanker(
    private val config: RankingConfig = RankingConfig(),
) {
    fun rank(
        candidates: List<MergedCandidate>,
        limit: Int,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): List<PredictionCandidate> {
        if (limit <= 0 || candidates.isEmpty()) return emptyList()

        val lmScores = normalizedLmScores(candidates)
        val maxFrequency = candidates.maxOfOrNull {
            it.selectionCount.coerceAtLeast(0)
        }?.takeIf { it > 0 } ?: 1

        return candidates.mapIndexed { index, candidate ->
            val frequency =
                candidate.selectionCount.coerceAtLeast(0).toFloat() / maxFrequency
            val recency = candidate.lastSelectedAtEpochMillis?.let { selectedAt ->
                val ageMillis = (nowEpochMillis - selectedAt).coerceAtLeast(0L)
                val ageDays = ageMillis.toDouble() / MILLIS_PER_DAY
                0.5.pow(ageDays / config.recencyHalfLifeDays).toFloat()
            } ?: 0f
            val score =
                config.lmWeight * lmScores[index] +
                    config.frequencyWeight * frequency +
                    config.recencyWeight * recency

            PredictionCandidate(text = candidate.text, score = score)
        }.sortedWith(
            compareByDescending<PredictionCandidate>(PredictionCandidate::score)
                .thenBy(PredictionCandidate::text)
        ).take(limit)
    }

    private fun normalizedLmScores(candidates: List<MergedCandidate>): List<Float> {
        val present = candidates.mapNotNull(MergedCandidate::lmLogProbability)
        if (present.isEmpty()) return List(candidates.size) { config.missingLmScore }

        val max = present.max()
        val exponentials = candidates.map { candidate ->
            candidate.lmLogProbability?.let { exp((it - max).toDouble()) }
        }
        val total = exponentials.filterNotNull().sum().takeIf { it > 0.0 } ?: 1.0
        return exponentials.map { value ->
            value?.let { (it / total).toFloat() } ?: config.missingLmScore
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000.0
    }
}
