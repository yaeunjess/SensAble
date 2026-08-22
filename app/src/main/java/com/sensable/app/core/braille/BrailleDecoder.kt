package com.sensable.app.core.braille

/**
 * 점 번호는 표준 점자 기준 (읽기 방향):
 *
 * [1] [4]
 * [2] [5]
 * [3] [6]
 *
 * UI 버튼은 쓰기 방향([4][1]/[5][2]/[6][3])으로 배치되어 있으나,
 * 버튼을 누르면 표준 점 번호(1~6)가 그대로 전달되므로 디코더는 표준 번호로 처리.
 */

object BrailleDecoder {

    /**
     * 숫자 디코딩 테이블 (표준 점자 기준).
     */
    private val numberTable: Map<Set<Int>, Char> = mapOf(
        setOf(1) to '1',
        setOf(1, 2) to '2',
        setOf(1, 4) to '3',
        setOf(1, 4, 5) to '4',
        setOf(1, 5) to '5',
        setOf(1, 2, 4) to '6',
        setOf(1, 2, 4, 5) to '7',
        setOf(1, 2, 5) to '8',
        setOf(2, 4) to '9',
        setOf(2, 4, 5) to '0',
    )

    fun decodeNumber(dots: Set<Int>): Char? = numberTable[dots]
}
