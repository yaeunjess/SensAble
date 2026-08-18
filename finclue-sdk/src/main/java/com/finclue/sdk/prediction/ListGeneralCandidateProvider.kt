package com.finclue.sdk.prediction

import java.text.Normalizer

internal class ListGeneralCandidateProvider(values: Sequence<String>) : GeneralCandidateProvider {
    private val candidates = values.map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { Entry(displayText = it, normalizedText = normalize(it)) }
        .distinctBy(Entry::normalizedText)
        .sortedBy(Entry::normalizedText)
        .toList()

    override fun findByPrefix(prefix: String, limit: Int): List<String> {
        if (limit <= 0) return emptyList()
        val normalizedPrefix = normalize(prefix)
        if (normalizedPrefix.isEmpty()) return emptyList()

        val start = lowerBound(normalizedPrefix)
        return buildList {
            for (index in start until candidates.size) {
                val entry = candidates[index]
                if (!entry.normalizedText.startsWith(normalizedPrefix)) break
                if (entry.normalizedText != normalizedPrefix) add(entry.displayText)
                if (size == limit) break
            }
        }
    }

    private fun lowerBound(value: String): Int {
        var low = 0
        var high = candidates.size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (candidates[middle].normalizedText < value) low = middle + 1 else high = middle
        }
        return low
    }

    private data class Entry(val displayText: String, val normalizedText: String)

    private companion object {
        fun normalize(value: String): String =
            Normalizer.normalize(value.trim(), Normalizer.Form.NFC).lowercase()
    }
}

