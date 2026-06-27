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
            BrailleMode.TRANSFER_RECIPIENT, BrailleMode.TRANSFER_AMOUNT -> toggleDot(dot)
            BrailleMode.TYPO_CORRECTION -> Unit
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

        // 오타교정 단계: 오른쪽 스와이프 = 현재 표시된 이름(후보 or 원본)으로 확정 → 금액 입력
        if (state.mode == BrailleMode.TYPO_CORRECTION) {
            val confirmedName = if (state.currentSuggestionIndex >= 0 && state.correctionSuggestions.isNotEmpty()) {
                state.correctionSuggestions[state.currentSuggestionIndex]
            } else {
                state.recipientName
            }
            ttsManager.speak("${confirmedName}님에게 얼마를 보낼까요?")
            _uiState.update {
                it.copy(
                    mode = BrailleMode.TRANSFER_AMOUNT,
                    guideMessage = "얼마를 보낼까요?",
                    recipientName = confirmedName,
                    inputText = "",
                    pendingDisplay = "",
                    currentCellDots = emptySet(),
                    isNumberMode = true,
                    confirmedCells = emptyList(),
                    correctionSuggestions = emptyList(),
                    currentSuggestionIndex = -1,
                )
            }
            return
        }
        val dots = state.currentCellDots
        if (dots.isEmpty()) return

        // 수취인 모드에서만 수표 감지 (금액 모드는 자동 숫자 모드)
        if (state.mode == BrailleMode.TRANSFER_RECIPIENT &&
            BrailleDecoder.isNumberPrefix(dots) && !state.isNumberMode
        ) {
            _uiState.update {
                it.copy(
                    currentCellDots = emptySet(),
                    isNumberMode = true,
                    confirmedCells = it.confirmedCells + listOf(dots),
                )
            }
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
                confirmedCells = it.confirmedCells + listOf(dots),
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
                ttsManager.speak("오타 교정을 하시겠습니까? 교정을 하시려면 위로 스와이프, 하지 않으시려면 오른쪽 스와이프를 해주세요.")
                _uiState.update {
                    it.copy(
                        mode = BrailleMode.TYPO_CORRECTION,
                        guideMessage = "오타 교정을 하시겠습니까?",
                        recipientName = finalText,
                        inputText = "",
                        pendingDisplay = "",
                        currentCellDots = emptySet(),
                        confirmedCells = emptyList(),
                        correctionSuggestions = emptyList(),
                        currentSuggestionIndex = -1,
                    )
                }
            }
            BrailleMode.TYPO_CORRECTION -> Unit
            BrailleMode.TRANSFER_AMOUNT -> {
                onNavigateToConfirm(state.recipientName, finalText)
            }
            BrailleMode.SERVICE_SELECT -> Unit
        }
    }

    // true를 반환하면 호출자가 바텀시트를 닫아야 함
    fun onSwipeLeft(): Boolean {
        val state = _uiState.value
        if (state.mode == BrailleMode.SERVICE_SELECT) return true
        if (state.mode == BrailleMode.TYPO_CORRECTION) {
            koreanStateMachine.reset()
            ttsManager.speak("누구에게 보낼까요?")
            _uiState.update {
                it.copy(
                    mode = BrailleMode.TRANSFER_RECIPIENT,
                    guideMessage = "누구에게 보낼까요?",
                    inputText = "",
                    pendingDisplay = "",
                    currentCellDots = emptySet(),
                    isNumberMode = false,
                    confirmedCells = emptyList(),
                    correctionSuggestions = emptyList(),
                    currentSuggestionIndex = -1,
                )
            }
            return false
        }

        // 1. 현재 셀에 점이 선택된 상태 → 셀만 초기화
        if (state.currentCellDots.isNotEmpty()) {
            ttsManager.speak("취소")
            _uiState.update { it.copy(currentCellDots = emptySet()) }
            return false
        }

        // 2 & 3. 확정된 입력(셀)이 있으면 → 마지막 셀 하나 되돌리기
        val cells = state.confirmedCells
        if (cells.isNotEmpty()) {
            val newCells = cells.dropLast(1)
            val rebuilt = replayCells(newCells, state.mode)
            val ttsText = rebuilt.pendingDisplay.ifEmpty {
                rebuilt.inputText.ifEmpty { "모두 지워졌습니다" }
            }
            ttsManager.speak(ttsText)
            _uiState.update {
                it.copy(
                    inputText = rebuilt.inputText,
                    pendingDisplay = rebuilt.pendingDisplay,
                    currentCellDots = emptySet(),
                    isNumberMode = rebuilt.isNumberMode,
                    confirmedCells = newCells,
                )
            }
            return false
        }

        // 4. 모두 비어있으면 → 이전 단계로 복귀
        when (state.mode) {
            BrailleMode.TRANSFER_RECIPIENT -> {
                koreanStateMachine.reset()
                ttsManager.speak("어떤 서비스를 이용하시겠습니까?")
                _uiState.update { BrailleUiState() }
            }
            BrailleMode.TRANSFER_AMOUNT -> {
                koreanStateMachine.reset()
                ttsManager.speak("누구에게 보낼까요?")
                _uiState.update {
                    it.copy(
                        mode = BrailleMode.TRANSFER_RECIPIENT,
                        guideMessage = "누구에게 보낼까요?",
                        inputText = "",
                        pendingDisplay = "",
                        currentCellDots = emptySet(),
                        isNumberMode = false,
                        confirmedCells = emptyList(),
                    )
                }
            }
            BrailleMode.SERVICE_SELECT -> return true
            BrailleMode.TYPO_CORRECTION -> Unit
        }
        return false
    }

    // 셀 목록을 처음부터 재연산해서 현재 inputText / pendingDisplay / isNumberMode를 복원
    private fun replayCells(cells: List<Set<Int>>, mode: BrailleMode): ReplayResult {
        koreanStateMachine.reset()
        var inputText = ""
        var pendingDisplay = ""
        var isNumberMode = mode == BrailleMode.TRANSFER_AMOUNT

        for (cell in cells) {
            if (!isNumberMode && BrailleDecoder.isNumberPrefix(cell)) {
                isNumberMode = true
                continue
            }
            if (isNumberMode) {
                inputText += BrailleDecoder.decodeNumber(cell)?.toString() ?: ""
            } else {
                inputText += koreanStateMachine.process(cell)
                pendingDisplay = koreanStateMachine.getPendingDisplay()
            }
        }
        return ReplayResult(inputText, pendingDisplay, isNumberMode)
    }

    private data class ReplayResult(
        val inputText: String,
        val pendingDisplay: String,
        val isNumberMode: Boolean,
    )

    fun onSwipeUp() {
        val state = _uiState.value
        if (state.mode != BrailleMode.TYPO_CORRECTION) return

        // 후보 목록이 없으면 목업 API에서 불러옴
        val suggestions = state.correctionSuggestions.ifEmpty {
            getMockCorrectionSuggestions(state.recipientName)
        }

        if (suggestions.isEmpty()) {
            ttsManager.speak("추천 결과가 없습니다.")
            return
        }

        val nextIndex = (state.currentSuggestionIndex + 1) % suggestions.size
        val suggestion = suggestions[nextIndex]
        ttsManager.speak(suggestion)

        _uiState.update {
            it.copy(
                correctionSuggestions = suggestions,
                currentSuggestionIndex = nextIndex,
                guideMessage = suggestion,
            )
        }
    }

    // 목업 교정 후보 — 실제 서비스에서는 백엔드 API 호출로 대체
    private fun getMockCorrectionSuggestions(input: String): List<String> {
        return listOf(
            "김봄", "김보", "김부", "이봄", "이보"
        ).filter { it != input }
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
    val confirmedCells: List<Set<Int>> = emptyList(),
    val correctionSuggestions: List<String> = emptyList(),
    val currentSuggestionIndex: Int = -1,
)

enum class BrailleMode {
    SERVICE_SELECT,
    TRANSFER_RECIPIENT,
    TYPO_CORRECTION,
    TRANSFER_AMOUNT
}
