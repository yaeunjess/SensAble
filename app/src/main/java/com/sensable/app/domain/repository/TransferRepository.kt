package com.sensable.app.domain.repository

import com.sensable.app.domain.model.TransferInfo

interface TransferRepository {
    suspend fun processTransfer(transferInfo: TransferInfo): Result<Unit>
}