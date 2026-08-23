package com.sensable.app.feature.kakaobank.viewmodel

import androidx.lifecycle.ViewModel
import com.sensable.app.core.common.AccountBalanceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class KakaoBankViewModel @Inject constructor(
    accountBalanceStore: AccountBalanceStore,
) : ViewModel() {

    val balance: StateFlow<Long> = accountBalanceStore.balance
}
