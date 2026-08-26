package com.sensable.app.core.braille

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KoreanBrailleStateMachineTest {

    @Test
    fun initialSiotIsAcceptedImmediatelyAndBuildsSa() {
        val machine = KoreanBrailleStateMachine()

        machine.process(setOf(6)) // ㅅ
        assertTrue(machine.wasLastInputAccepted)
        assertEquals("ㅅ", machine.getPendingDisplay())

        machine.process(setOf(1, 2, 6)) // ㅏ
        assertEquals("사", machine.getPendingDisplay())
    }

    @Test
    fun explicitInitialIeungBuildsA() {
        val machine = KoreanBrailleStateMachine()

        machine.process(setOf(1, 2, 4, 5)) // 명시적 초성 ㅇ
        machine.process(setOf(1, 2, 6)) // ㅏ

        assertEquals("아", machine.getPendingDisplay())
    }

    @Test
    fun finalPieupBuildsSup() {
        val machine = KoreanBrailleStateMachine()

        machine.process(setOf(6)) // ㅅ
        machine.process(setOf(1, 3, 4)) // ㅜ
        val committed = machine.process(setOf(2, 5, 6)) // 받침 ㅍ

        assertEquals("숲", committed)
        assertTrue(machine.wasLastInputAccepted)
    }

    @Test
    fun invalidCellAfterInitialIsNotAcceptedOrAddedToPendingDisplay() {
        val machine = KoreanBrailleStateMachine()

        machine.process(setOf(4)) // ㄱ
        assertTrue(machine.wasLastInputAccepted)
        assertEquals("ㄱ", machine.getPendingDisplay())

        machine.process(setOf(4)) // 중성 위치에는 올 수 없는 셀

        assertFalse(machine.wasLastInputAccepted)
        assertEquals("ㄱ", machine.getPendingDisplay())
    }
}
