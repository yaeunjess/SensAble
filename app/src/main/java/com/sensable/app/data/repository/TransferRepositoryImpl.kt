package com.sensable.app.data.repository

import com.sensable.app.domain.model.TransferInfo
import com.sensable.app.domain.repository.TransferRepository
import javax.inject.Inject

class TransferRepositoryImpl @Inject constructor() : TransferRepository {

    override suspend fun processTransfer(transferInfo: TransferInfo): Result<Unit> {
        // Mockup — no real API call
        return Result.success(Unit)
    }
}