package com.sensable.app.feature.braille.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BrailleViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BrailleUiState())
    val uiState: StateFlow<BrailleUiState> = _uiState.asStateFlow()

    fun onBrailleButtonClick(row: Int, col: Int, onNavigateToConfirm: () -> Unit) {
        when (_uiState.value.mode) {
            BrailleMode.SERVICE_SELECT -> handleServiceSelect(row, col)
            BrailleMode.TRANSFER_RECIPIENT -> handleRecipientInput()
            BrailleMode.TRANSFER_AMOUNT -> handleAmountInput(onNavigateToConfirm)
        }
    }

    private fun handleServiceSelect(row: Int, col: Int) {
        if (row == 0 && col == 0) {
            _uiState.update {
                it.copy(
                    mode = BrailleMode.TRANSFER_RECIPIENT,
                    guideMessage = "누구에게 보낼까요?"
                )
            }
        }
    }

    private fun handleRecipientInput() {
        _uiState.update {
            it.copy(
                mode = BrailleMode.TRANSFER_AMOUNT,
                guideMessage = "000에게 얼마를 보낼까요?"
            )
        }
    }

    private fun handleAmountInput(onNavigateToConfirm: () -> Unit) {
        onNavigateToConfirm()
    }

    fun reset() {
        _uiState.update { BrailleUiState() }
    }
}

data class BrailleUiState(
    val mode: BrailleMode = BrailleMode.SERVICE_SELECT,
    val guideMessage: String = "어떤 서비스를 이용하시겠습니까?"
)

enum class BrailleMode {
    SERVICE_SELECT,
    TRANSFER_RECIPIENT,
    TRANSFER_AMOUNT
}