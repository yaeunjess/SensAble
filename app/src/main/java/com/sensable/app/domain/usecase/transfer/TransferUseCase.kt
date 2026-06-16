package com.sensable.app.domain.usecase.transfer

import com.sensable.app.domain.model.TransferInfo
import com.sensable.app.domain.repository.TransferRepository
import javax.inject.Inject

class TransferUseCase @Inject constructor(
    private val repository: TransferRepository
) {
    suspend operator fun invoke(transferInfo: TransferInfo): Result<Unit> =
        repository.processTransfer(transferInfo)
}