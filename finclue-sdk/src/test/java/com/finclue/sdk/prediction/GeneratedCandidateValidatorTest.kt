package com.finclue.sdk.prediction

import com.finclue.sdk.api.PredictionContext
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratedCandidateValidatorTest {
    @Test
    fun `keeps complete Korean names separated by commas`() {
        val candidates = GeneratedCandidateValidator.validate(
            PredictionContext.PERSON_NAME,
            prefix = "현",
            value = "현석호, 현준혁, 현우",
        ).toList()

        assertEquals(listOf("현석호", "현준혁", "현우"), candidates)
    }

    @Test
    fun `rejects Korean prose that merely starts with the prefix`() {
        val candidates = sequenceOf("현위치에서", "현기증나게", "현석호입니다")
            .flatMap {
                GeneratedCandidateValidator.validate(
                    PredictionContext.PERSON_NAME,
                    prefix = "현",
                    value = it,
                )
            }
            .toList()

        assertEquals(emptyList<String>(), candidates)
    }
}
