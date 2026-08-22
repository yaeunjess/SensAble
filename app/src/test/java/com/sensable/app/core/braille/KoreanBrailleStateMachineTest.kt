package com.sensable.app.core.braille

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KoreanBrailleStateMachineTest {

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
