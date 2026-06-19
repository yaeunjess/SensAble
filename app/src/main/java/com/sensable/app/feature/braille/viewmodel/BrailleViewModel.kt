package com.sensable.app.feature.braille.viewmodel

import androidx.lifecycle.ViewModel
import com.sensable.app.core.braille.BrailleDecoder
import com.sensable.app.core.braille.KoreanBrailleStateMachine
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

    private val koreanStateMachine = KoreanBrailleStateMachine()

    fun onBrailleButtonClick(dot: Int) {
        when (_uiState.value.mode) {
            BrailleMode.SERVICE_SELECT -> handleServiceSelect(dot)
            BrailleMode.TRANSFER_RECIPIENT,
            BrailleMode.TRANSFER_AMOUNT -> toggleDot(dot)
        }
    }

    private fun handleServiceSelect(dot: Int) {
        if (dot == 1) {
            _uiState.update {
                it.copy(
                    mode = BrailleMode.TRANSFER_RECIPIENT,
                    guideMessage = "누구에게 보낼까요?"
                )
            }
        }
    }

    private fun toggleDot(dot: Int) {
        val current = _uiState.value.currentCellDots
        val updated = if (dot in current) current - dot else current + dot
        _uiState.update { it.copy(currentCellDots = updated) }
    }

    fun onSwipeRight() {
        val state = _uiState.value
        if (state.mode == BrailleMode.SERVICE_SELECT) return
        val dots = state.currentCellDots
        if (dots.isEmpty()) return

        // 수취인 모드에서만 수표 감지 (금액 모드는 자동 숫자 모드)
        if (state.mode == BrailleMode.TRANSFER_RECIPIENT &&
            BrailleDecoder.isNumberPrefix(dots) && !state.isNumberMode
        ) {
            _uiState.update { it.copy(currentCellDots = emptySet(), isNumberMode = true) }
            return
        }

        val decoded: String = if (state.isNumberMode) {
            BrailleDecoder.decodeNumber(dots)?.toString() ?: ""
        } else {
            koreanStateMachine.process(dots)
        }

        val newText = state.inputText + decoded
        _uiState.update {
            it.copy(
                currentCellDots = emptySet(),
                inputText = newText,
                pendingDisplay = if (!state.isNumberMode) koreanStateMachine.getPendingDisplay() else "",
            )
        }
    }

    // onNavigateToConfirm: (recipient, amount) -> Unit
    fun onDoubleTap(onNavigateToConfirm: (String, String) -> Unit) {
        val state = _uiState.value

        val flushed = if (!state.isNumberMode) koreanStateMachine.flush() else ""
        val finalText = state.inputText + flushed

        when (state.mode) {
            BrailleMode.TRANSFER_RECIPIENT -> {
                _uiState.update {
                    it.copy(
                        mode = BrailleMode.TRANSFER_AMOUNT,
                        guideMessage = "얼마를 보낼까요?",
                        recipientName = finalText,
                        inputText = "",
                        pendingDisplay = "",
                        currentCellDots = emptySet(),
                        isNumberMode = true,  // 금액은 항상 숫자 — prefix 불필요
                    )
                }
            }
            BrailleMode.TRANSFER_AMOUNT -> {
                onNavigateToConfirm(state.recipientName, finalText)
            }
            BrailleMode.SERVICE_SELECT -> Unit
        }
    }

    fun reset() {
        koreanStateMachine.reset()
        _uiState.update { BrailleUiState() }
    }
}

data class BrailleUiState(
    val mode: BrailleMode = BrailleMode.SERVICE_SELECT,
    val guideMessage: String = "어떤 서비스를 이용하시겠습니까?",
    val currentCellDots: Set<Int> = emptySet(),
    val isNumberMode: Boolean = false,
    val inputText: String = "",
    val pendingDisplay: String = "",
    val recipientName: String = "",
)

enum class BrailleMode {
    SERVICE_SELECT,
    TRANSFER_RECIPIENT,
    TRANSFER_AMOUNT
}
