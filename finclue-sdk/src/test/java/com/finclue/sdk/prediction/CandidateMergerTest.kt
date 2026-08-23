package com.finclue.sdk.prediction

import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateMergerTest {
    @Test
    fun `merges duplicate LM and personal candidates without losing either score`() {
        val result = CandidateMerger.merge(
            lmCandidates = listOf(
                LmCandidate("국민은행", -1.4f),
                LmCandidate(" 국민은행 ", -2.0f),
                LmCandidate("국민연금", -2.1f),
            ),
            personalCandidates = listOf(
                PersonalCandidate("국민은행", 3, 100L),
                PersonalCandidate("국민카드", 8, 200L),
            ),
        )

        assertEquals(3, result.size)
        assertEquals(-1.4f, result.single { it.text == "국민은행" }.lmLogProbability)
        assertEquals(3, result.single { it.text == "국민은행" }.selectionCount)
        assertEquals(null, result.single { it.text == "국민카드" }.lmLogProbability)
    }
}

