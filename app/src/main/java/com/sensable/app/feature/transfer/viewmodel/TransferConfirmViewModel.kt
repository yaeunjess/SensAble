package com.sensable.app.feature.transfer.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class TransferConfirmViewModel @Inject constructor() : ViewModel() {

    private val _showFingerprintSheet = MutableStateFlow(false)
    val showFingerprintSheet: StateFlow<Boolean> = _showFingerprintSheet.asStateFlow()

    fun onAuthButtonClick() {
        _showFingerprintSheet.value = true
    }

    fun onSheetDismissed() {
        _showFingerprintSheet.value = false
    }
}