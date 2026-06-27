package com.sensable.app.core.common

const val MOCK_BALANCE = 500_000L

fun postTransferBalance(amount: String): Long =
    (MOCK_BALANCE - (amount.toLongOrNull() ?: 0L)).coerceAtLeast(0L)
