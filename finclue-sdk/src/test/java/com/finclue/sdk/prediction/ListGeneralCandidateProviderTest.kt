package com.finclue.sdk.prediction

import org.junit.Assert.assertEquals
import org.junit.Test

class ListGeneralCandidateProviderTest {
    private val provider = ListGeneralCandidateProvider(
        sequenceOf(
            "국민은행",
            "국민연금",
            "# comment",
            "국민카드",
            "국민은행",
            "신한은행",
        )
    )

    @Test
    fun `finds sorted distinct candidates by normalized prefix`() {
        assertEquals(
            listOf("국민연금", "국민은행", "국민카드"),
            provider.findByPrefix(" 국민 ", limit = 10),
        )
    }

    @Test
    fun `honors limit and rejects empty prefix`() {
        assertEquals(2, provider.findByPrefix("국민", limit = 2).size)
        assertEquals(emptyList<String>(), provider.findByPrefix(" ", limit = 10))
    }
}

