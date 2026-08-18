package com.finclue.sdk.prediction

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalRankerTest {
    private val now = 1_800_000_000_000L

    @Test
    fun `repeated recent personal selection can move candidate to first place`() {
        val candidates = CandidateMerger.merge(
            lmCandidates = listOf(
                LmCandidate("국민은행", -1.45f),
                LmCandidate("국민연금", -2.10f),
                LmCandidate("국민카드", -2.39f),
            ),
            personalCandidates = listOf(
                PersonalCandidate("국민카드", selectionCount = 15, lastSelectedAtEpochMillis = now),
                PersonalCandidate("국민은행", selectionCount = 3, lastSelectedAtEpochMillis = now - DAY),
            ),
        )

        val ranked = PersonalRanker().rank(candidates, limit = 3, nowEpochMillis = now)

        assertEquals("국민카드", ranked.first().text)
        assertEquals(listOf("국민카드", "국민은행", "국민연금"), ranked.map { it.text })
    }

    @Test
    fun `personal-only candidate remains eligible with default LM score`() {
        val candidates = CandidateMerger.merge(
            lmCandidates = listOf(LmCandidate("국민은행", -1.5f)),
            personalCandidates = listOf(
                PersonalCandidate("국민카드", selectionCount = 20, lastSelectedAtEpochMillis = now)
            ),
        )

        val ranked = PersonalRanker().rank(candidates, limit = 2, nowEpochMillis = now)

        assertEquals("국민카드", ranked.first().text)
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}

