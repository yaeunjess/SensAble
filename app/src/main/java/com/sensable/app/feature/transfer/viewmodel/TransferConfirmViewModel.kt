package com.sensable.app.feature.transfer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferConfirmViewModel @Inject constructor() : ViewModel() {

    private val _showFingerprintSheet = MutableStateFlow(false)
    val showFingerprintSheet: StateFlow<Boolean> = _showFingerprintSheet.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1500)
            _showFingerprintSheet.value = true
        }
    }

    fun onAuthButtonClick() {
        _showFingerprintSheet.value = true
    }

    fun onSheetDismissed() {
        _showFingerprintSheet.value = false
    }
}