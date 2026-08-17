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
}
