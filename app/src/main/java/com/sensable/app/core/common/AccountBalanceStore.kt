package com.sensable.app.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 프로세스가 살아있는 동안만 유지되는 잔액 상태 — 재실행하면 [MOCK_BALANCE]로 초기화됨 */
@Singleton
class AccountBalanceStore @Inject constructor() {
    private val _balance = MutableStateFlow(MOCK_BALANCE)
    val balance: StateFlow<Long> = _balance.asStateFlow()

    fun deduct(amount: Long) {
        _balance.update { (it - amount).coerceAtLeast(0L) }
    }
}
