package com.finclue.sdk.internal

internal object BrailleNumberDecoder {
    private val numberTable = mapOf(
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

    fun decode(dots: Set<Int>): Char? = numberTable[dots]
}
