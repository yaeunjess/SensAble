package com.finclue.sdk.api

import org.junit.Assert.assertThrows
import org.junit.Test

class PredictionRequestTest {
    @Test
    fun `history access requires personalized mode`() {
        assertThrows(IllegalArgumentException::class.java) {
            PredictionRequest(
                currentText = "김",
                mode = PredictionMode.GENERAL,
                context = PredictionContext.PERSON_NAME,
                historyPolicy = HistoryPolicy.READ_WRITE,
            )
        }
    }

    @Test
    fun `request rejects blank input before loading the model`() {
        assertThrows(IllegalArgumentException::class.java) {
            PredictionRequest(currentText = " ")
        }
    }
}
