package com.sensable.app.core.braille

import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanBrailleStateMachineTest {
    @Test
    fun `shows simple vowel preview while waiting for possible compound vowel`() {
        val machine = KoreanBrailleStateMachine()

        assertEquals("", machine.process(setOf(4, 6)))
        assertEquals("ㅈ", machine.getPendingDisplay())

        assertEquals("", machine.process(setOf(1, 3, 4)))
        assertEquals("주", machine.getPendingDisplay())
    }

    @Test
    fun `composes 현준혁 without exposing standalone jamo`() {
        val machine = KoreanBrailleStateMachine()
        val committed = buildString {
            append(machine.process(setOf(2, 4, 5)))    // ㅎ
            append(machine.process(setOf(1, 5, 6)))    // ㅕ
            append(machine.process(setOf(2, 5)))       // ㄴ: 현
            assertEquals("현", toString())

            append(machine.process(setOf(4, 6)))       // ㅈ
            assertEquals("ㅈ", machine.getPendingDisplay())
            append(machine.process(setOf(1, 3, 4)))    // ㅜ (ㅟ 가능성 대기)
            assertEquals("주", machine.getPendingDisplay())
            append(machine.process(setOf(2, 5)))       // ㄴ: 준
            assertEquals("현준", toString())

            append(machine.process(setOf(2, 4, 5)))    // ㅎ
            append(machine.process(setOf(1, 5, 6)))    // ㅕ
            append(machine.process(setOf(1)))          // ㄱ: 혁
        }

        assertEquals("현준혁", committed)
        assertEquals("", machine.getPendingDisplay())
    }
}
