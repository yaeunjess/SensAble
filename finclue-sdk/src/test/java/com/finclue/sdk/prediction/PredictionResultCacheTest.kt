package com.finclue.sdk.prediction

import com.finclue.sdk.api.HistoryPolicy
import com.finclue.sdk.api.PredictionCandidate
import com.finclue.sdk.api.PredictionContext
import com.finclue.sdk.api.PredictionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PredictionResultCacheTest {
    @Test
    fun `evicts least recently used result`() {
        val cache = PredictionResultCache(maxEntries = 2)
        val first = key(PredictionContext.PERSON_NAME, "현")
        val second = key(PredictionContext.PERSON_NAME, "김")
        val third = key(PredictionContext.BANK_NAME, "국민")

        cache[first] = listOf(PredictionCandidate("현준", 1f))
        cache[second] = listOf(PredictionCandidate("김보미", 1f))
        cache[first]
        cache[third] = listOf(PredictionCandidate("국민은행", 1f))

        assertNull(cache[second])
        assertEquals("현준", cache[first]?.single()?.text)
    }

    @Test
    fun `invalidates only matching context`() {
        val cache = PredictionResultCache(maxEntries = 4)
        val person = key(PredictionContext.PERSON_NAME, "현")
        val bank = key(PredictionContext.BANK_NAME, "국민")
        cache[person] = listOf(PredictionCandidate("현준", 1f))
        cache[bank] = listOf(PredictionCandidate("국민은행", 1f))

        cache.invalidate(PredictionContext.PERSON_NAME)

        assertNull(cache[person])
        assertEquals("국민은행", cache[bank]?.single()?.text)
    }

    private fun key(context: PredictionContext, prefix: String) = PredictionCacheKey(
        context = context,
        normalizedPrefix = prefix,
        mode = PredictionMode.PERSONALIZED,
        historyPolicy = HistoryPolicy.READ_WRITE,
        limit = 3,
    )
}
