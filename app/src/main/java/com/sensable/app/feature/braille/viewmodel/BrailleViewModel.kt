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

    fun onBrailleButtonClick(row: Int, col: Int, onTransferSelected: () -> Unit) {
        when (_uiState.value.mode) {
            BrailleMode.SERVICE_SELECT -> handleServiceSelect(row, col, onTransferSelected)
        }
    }

    private fun handleServiceSelect(row: Int, col: Int, onTransferSelected: () -> Unit) {
        val isTopLeft = row == 0 && col == 0
        if (isTopLeft) {
            _uiState.update { it.copy(mode = BrailleMode.SERVICE_SELECT) }
            onTransferSelected()
        }
    }
}

data class BrailleUiState(
    val mode: BrailleMode = BrailleMode.SERVICE_SELECT,
    val guideMessage: String = "어떤 서비스를 이용하시겠습니까?"
)

enum class BrailleMode {
    SERVICE_SELECT
}