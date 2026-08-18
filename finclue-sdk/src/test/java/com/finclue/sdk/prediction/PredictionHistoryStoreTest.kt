package com.finclue.sdk.prediction

import org.junit.Assert.assertEquals
import org.junit.Test

class PredictionHistoryStoreTest {
    @Test
    fun `normalizes whitespace case and unicode composition`() {
        val decomposed = "  GUGMIN  "

        assertEquals("gugmin", PredictionHistoryStore.normalize(decomposed))
        assertEquals("국민은행", PredictionHistoryStore.normalize("  국민은행  "))
    }
}

