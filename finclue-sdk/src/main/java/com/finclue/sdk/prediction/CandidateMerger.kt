package com.finclue.sdk.prediction

import java.text.Normalizer

internal object CandidateMerger {
    fun merge(
        lmCandidates: List<LmCandidate>,
        personalCandidates: List<PersonalCandidate>,
    ): List<MergedCandidate> {
        val merged = linkedMapOf<String, MergedCandidate>()

        lmCandidates.forEach { candidate ->
            val normalized = normalize(candidate.text)
            if (normalized.isNotEmpty()) {
                val existing = merged[normalized]
                merged[normalized] = MergedCandidate(
                    text = candidate.text.trim(),
                    normalizedText = normalized,
                    lmLogProbability = maxOf(
                        candidate.logProbability,
                        existing?.lmLogProbability ?: Float.NEGATIVE_INFINITY,
                    ),
                    selectionCount = existing?.selectionCount ?: 0,
                    lastSelectedAtEpochMillis = existing?.lastSelectedAtEpochMillis,
                )
            }
        }

        personalCandidates.forEach { candidate ->
            val normalized = normalize(candidate.text)
            if (normalized.isNotEmpty()) {
                val existing = merged[normalized]
                merged[normalized] = MergedCandidate(
                    text = existing?.text ?: candidate.text.trim(),
                    normalizedText = normalized,
                    lmLogProbability = existing?.lmLogProbability,
                    selectionCount = candidate.selectionCount,
                    lastSelectedAtEpochMillis = candidate.lastSelectedAtEpochMillis,
                )
            }
        }

        return merged.values.toList()
    }

    internal fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFC).lowercase()
}

