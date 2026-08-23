package com.finclue.sdk.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FlowSpecTest {
    @Test
    fun `accepts fields with unique keys`() {
        val spec = FlowSpec(
            fields = listOf(
                FieldSpec("account", FieldType.ACCOUNT, "받는 계좌", "계좌번호를 입력하세요"),
                FieldSpec("amount", FieldType.AMOUNT, "보낼 금액", "금액을 입력하세요"),
            )
        )

        assertEquals(2, spec.fields.size)
    }

    @Test
    fun `rejects duplicate field keys`() {
        assertThrows(IllegalArgumentException::class.java) {
            FlowSpec(
                fields = listOf(
                    FieldSpec("value", FieldType.ACCOUNT, "첫 번째", "입력하세요"),
                    FieldSpec("value", FieldType.AMOUNT, "두 번째", "입력하세요"),
                )
            )
        }
    }

    @Test
    fun `prediction and history are disabled by default`() {
        val field = FieldSpec("memo", FieldType.ACCOUNT, "메모", "입력하세요")

        assertEquals(PredictionMode.DISABLED, field.predictionMode)
        assertEquals(PredictionContext.GENERAL, field.predictionContext)
        assertEquals(HistoryPolicy.NONE, field.historyPolicy)
    }

    @Test
    fun `accepts host controlled personalized history policy`() {
        val field = FieldSpec(
            key = "bank",
            type = FieldType.ACCOUNT,
            label = "은행",
            prompt = "은행명을 입력하세요",
            predictionMode = PredictionMode.PERSONALIZED,
            predictionContext = PredictionContext.BANK_NAME,
            historyPolicy = HistoryPolicy.READ_WRITE,
        )

        assertEquals(PredictionContext.BANK_NAME, field.predictionContext)
        assertEquals(HistoryPolicy.READ_WRITE, field.historyPolicy)
    }

    @Test
    fun `rejects history when prediction is not personalized`() {
        assertThrows(IllegalArgumentException::class.java) {
            FieldSpec(
                key = "bank",
                type = FieldType.ACCOUNT,
                label = "은행",
                prompt = "은행명을 입력하세요",
                predictionMode = PredictionMode.GENERAL,
                predictionContext = PredictionContext.BANK_NAME,
                historyPolicy = HistoryPolicy.READ_WRITE,
            )
        }
    }
}
