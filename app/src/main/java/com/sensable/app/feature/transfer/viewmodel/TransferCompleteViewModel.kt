package com.sensable.app.feature.transfer.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sensable.app.core.common.AccountBalanceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TransferCompleteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountBalanceStore: AccountBalanceStore,
) : ViewModel() {

    init {
        val amount = savedStateHandle.get<String>("amount") ?: ""
        accountBalanceStore.deduct(amount.toLongOrNull() ?: 0L)
    }
}
