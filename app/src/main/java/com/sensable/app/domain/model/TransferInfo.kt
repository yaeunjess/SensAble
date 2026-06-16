package com.sensable.app.domain.model

data class TransferInfo(
    val recipientName: String = "",
    val accountNumber: String = "",
    val amount: Long = 0L
)