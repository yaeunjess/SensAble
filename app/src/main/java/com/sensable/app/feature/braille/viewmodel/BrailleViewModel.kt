package com.sensable.app.feature.braille.viewmodel

import androidx.lifecycle.ViewModel
import com.sensable.app.core.braille.BrailleDecoder
import com.sensable.app.core.braille.KoreanBrailleStateMachine
import com.sensable.app.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BrailleViewModel @Inject constructor(
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrailleUiState())
    val uiState: StateFlow<BrailleUiState> = _uiState.asStateFlow()

    private val koreanStateMachine = KoreanBrailleStateMachine()

    init {
        ttsManager.speak("어떤 서비스를 이용하시겠습니까?")
    }

    fun onBrailleButtonClick(dot: Int) {
        ttsManager.speak(dot.toString())
        when (_uiState.value.mode) {
            BrailleMode.SERVICE_SELECT -> handleServiceSelect(dot)
            BrailleMode.TRANSFER_RECIPIENT,
            BrailleMode.TRANSFER_AMOUNT -> toggleDot(dot)
        }
    }

    private fun handleServiceSelect(dot: Int) {
        if (dot == 1) {
            ttsManager.speakQueued("누구에게 보낼까요?")
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
        if (dot in current) {
            ttsManager.speak("$dot 취소")
            _uiState.update { it.copy(currentCellDots = current - dot) }
        } else {
            _uiState.update { it.copy(currentCellDots = current + dot) }
        }
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

        val newPendingDisplay = if (!state.isNumberMode) koreanStateMachine.getPendingDisplay() else ""
        val newText = state.inputText + decoded

        if (decoded.isNotEmpty()) {
            val spoken = if (state.mode == BrailleMode.TRANSFER_AMOUNT) "${newText}원" else decoded
            ttsManager.speak(spoken)
            if (newPendingDisplay.isNotEmpty()) ttsManager.speakQueued(newPendingDisplay)
        } else if (newPendingDisplay.isNotEmpty()) {
            ttsManager.speak(newPendingDisplay)
        }
        _uiState.update {
            it.copy(
                currentCellDots = emptySet(),
                inputText = newText,
                pendingDisplay = newPendingDisplay,
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
                ttsManager.speak("${finalText}님에게 얼마를 보낼까요?")
                _uiState.update {
                    it.copy(
                        mode = BrailleMode.TRANSFER_AMOUNT,
                        guideMessage = "얼마를 보낼까요?",
                        recipientName = finalText,
                        inputText = "",
                        pendingDisplay = "",
                        currentCellDots = emptySet(),
                        isNumberMode = true,
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
