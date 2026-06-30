package com.sensable.app.feature.braille.viewmodel

import androidx.lifecycle.ViewModel
import com.sensable.app.core.braille.BrailleDecoder
import com.sensable.app.core.braille.KoreanBrailleStateMachine
import com.sensable.app.core.common.postTransferBalance
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
        ttsManager.speakQueued("오른쪽 스와이프를 통해 기능을 선택하세요.")
    }

    fun onBrailleButtonClick(dot: Int) {
        when (_uiState.value.mode) {
            BrailleMode.SERVICE_SELECT -> Unit
            BrailleMode.TRANSFER_RECIPIENT, BrailleMode.TRANSFER_AMOUNT -> {
                ttsManager.speak(dot.toString())
                toggleDot(dot)
            }
            BrailleMode.TYPO_CORRECTION -> Unit
            BrailleMode.TRANSFER_CONFIRM -> Unit
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
        if (state.mode == BrailleMode.SERVICE_SELECT) {
            val services = listOf(4 to "송금하기", 1 to "잔액조회")
            val currentDot = state.currentCellDots.firstOrNull()
            val currentIdx = services.indexOfFirst { it.first == currentDot }
            val nextIdx = (currentIdx + 1) % services.size
            val (nextDot, nextLabel) = services[nextIdx]
            ttsManager.speak(nextLabel)
            _uiState.update { it.copy(currentCellDots = setOf(nextDot)) }
            return
        }

        if (state.mode == BrailleMode.TYPO_CORRECTION) return
        val dots = state.currentCellDots
        if (dots.isEmpty()) return

        // 수취인 모드에서만 수표 감지 (금액 모드는 자동 숫자 모드)
        if (state.mode == BrailleMode.TRANSFER_RECIPIENT &&
            BrailleDecoder.isNumberPrefix(dots) && !state.isNumberMode
        ) {
            ttsManager.speak("계좌번호를 입력하세요.")
            _uiState.update {
                it.copy(
                    currentCellDots = emptySet(),
                    isNumberMode = true,
                    guideMessage = "계좌번호를 입력하세요.",
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

        // 수취인 첫 입력 시 자동완성 안내
        if (state.mode == BrailleMode.TRANSFER_RECIPIENT &&
            state.inputText.isEmpty() && state.pendingDisplay.isEmpty() &&
            (newText.isNotEmpty() || newPendingDisplay.isNotEmpty())
        ) {
            ttsManager.speakQueued("위로 스와이프하면 이름을 자동완성해 드립니다.")
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
            BrailleMode.SERVICE_SELECT -> {
                val activeDot = state.currentCellDots.firstOrNull()
                when (activeDot) {
                    4 -> {
                        ttsManager.speak("누구에게 보낼까요?")
                        _uiState.update {
                            it.copy(
                                mode = BrailleMode.TRANSFER_RECIPIENT,
                                guideMessage = "누구에게 보낼까요?",
                                currentCellDots = emptySet()
                            )
                        }
                    }
                    1 -> ttsManager.speak("잔액조회 기능은 준비 중입니다.")
                    else -> Unit
                }
            }
            BrailleMode.TRANSFER_RECIPIENT -> {
                // 계좌번호(숫자) 입력 시: 오타교정 건너뛰고 바로 금액 입력, 수취인 고정
                if (state.isNumberMode) {
                    val recipient = "이지영"
                    ttsManager.speak("${recipient}님에게 얼마를 보낼까요?")
                    _uiState.update {
                        it.copy(
                            mode = BrailleMode.TRANSFER_AMOUNT,
                            guideMessage = "얼마를 보낼까요?",
                            recipientName = recipient,
                            inputText = "",
                            pendingDisplay = "",
                            currentCellDots = emptySet(),
                            isNumberMode = true,
                            confirmedCells = emptyList(),
                            correctionSuggestions = emptyList(),
                            currentSuggestionIndex = -1,
                            autocompleteSuggestion = "",
                        )
                    }
                    return
                }
                // 한글(이름) 입력 시: 오타교정 단계로
                val confirmedName = if (state.currentSuggestionIndex >= 0 && state.correctionSuggestions.isNotEmpty()) {
                    state.correctionSuggestions[state.currentSuggestionIndex]
                } else {
                    finalText
                }
                ttsManager.speak("오타 교정을 원하시면 위 스와이프를 해주세요. 건너뛰려면 두 번 탭하세요.")
                _uiState.update {
                    it.copy(
                        mode = BrailleMode.TYPO_CORRECTION,
                        guideMessage = "오타 교정을 하시겠습니까?",
                        recipientName = confirmedName,
                        inputText = "",
                        pendingDisplay = "",
                        currentCellDots = emptySet(),
                        confirmedCells = emptyList(),
                        correctionSuggestions = emptyList(),
                        currentSuggestionIndex = -1,
                        autocompleteSuggestion = "",
                    )
                }
            }
            BrailleMode.TYPO_CORRECTION -> {
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
                        autocompleteSuggestion = "",
                    )
                }
            }
            BrailleMode.TRANSFER_AMOUNT -> {
                val recipient = state.recipientName
                val amountLong = finalText.toLongOrNull() ?: 0L
                val formattedAmount = "%,d원".format(amountLong)
                val postBalance = "%,d원".format(postTransferBalance(finalText))
                val confirmGuide = "${recipient}님에게 ${formattedAmount}을 보내시겠습니까?\n이체 후 잔액은 ${postBalance}입니다."
                ttsManager.speak("${recipient}님에게 ${formattedAmount}을 보내시겠습니까? 이체 후 잔액은 ${postBalance}입니다.")
                _uiState.update {
                    it.copy(
                        mode = BrailleMode.TRANSFER_CONFIRM,
                        guideMessage = confirmGuide,
                        transferAmount = finalText,
                        currentCellDots = emptySet(),
                        confirmedCells = emptyList(),
                    )
                }
            }
            BrailleMode.TRANSFER_CONFIRM -> {
                onNavigateToConfirm(state.recipientName, state.transferAmount)
            }
        }
    }

    // true를 반환하면 호출자가 바텀시트를 닫아야 함
    fun onSwipeLeft(): Boolean {
        val state = _uiState.value
        if (state.mode == BrailleMode.SERVICE_SELECT) return true

        // 자동완성 후보 탐색 중 → 후보 목록 닫고 입력 화면으로 복귀
        if (state.mode == BrailleMode.TRANSFER_RECIPIENT && state.currentSuggestionIndex >= 0) {
            val currentInput = state.inputText + state.pendingDisplay
            ttsManager.speak("누구에게 보낼까요?")
            if (currentInput.isNotEmpty()) {
                ttsManager.speakQueued("현재 '${currentInput}'가 입력되어 있습니다.")
            }
            _uiState.update {
                it.copy(
                    correctionSuggestions = emptyList(),
                    currentSuggestionIndex = -1,
                    autocompleteSuggestion = "",
                )
            }
            return false
        }

        if (state.mode == BrailleMode.TYPO_CORRECTION) {
            // 추천 목록 탐색 중 → 목록 닫고 오타교정 초기 상태로
            if (state.currentSuggestionIndex >= 0) {
                ttsManager.speak("오타 교정을 원하시면 위로 밀어 주세요. 건너뛰려면 두 번 탭하세요.")
                _uiState.update {
                    it.copy(
                        correctionSuggestions = emptyList(),
                        currentSuggestionIndex = -1,
                        autocompleteSuggestion = "",
                    )
                }
                return false
            }
            // 추천 목록 없는 상태 → 수취인 입력화면으로
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
                    autocompleteSuggestion = "",
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
                ttsManager.speak("어떤 서비스를 이용하시겠어요?")
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
            BrailleMode.TRANSFER_CONFIRM -> {
                val recipient = state.recipientName
                val formattedAmount = "%,d원".format(state.transferAmount.toLongOrNull() ?: 0L)
                ttsManager.speak("${recipient}님에게 얼마를 보낼까요?")
                _uiState.update {
                    it.copy(
                        mode = BrailleMode.TRANSFER_AMOUNT,
                        guideMessage = "얼마를 보낼까요?",
                        inputText = state.transferAmount,
                        pendingDisplay = "",
                        currentCellDots = emptySet(),
                        isNumberMode = true,
                        confirmedCells = emptyList(),
                        transferAmount = "",
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

        val suggestions = when (state.mode) {
            BrailleMode.TYPO_CORRECTION -> state.correctionSuggestions.ifEmpty {
                getMockCorrectionSuggestions(state.recipientName)
            }
            BrailleMode.TRANSFER_RECIPIENT -> state.correctionSuggestions.ifEmpty {
                getMockAutocompleteSuggestions()
            }
            else -> return
        }

        if (suggestions.isEmpty()) {
            ttsManager.speak("일치하는 이름을 찾지 못했어요.")
            return
        }

        val nextIndex = (state.currentSuggestionIndex + 1) % suggestions.size
        val suggestion = suggestions[nextIndex]
        ttsManager.speak(suggestion)

        // 두 모드 모두 autocompleteSuggestion에 저장 — UI에서 같은 자리에 표시
        _uiState.update {
            it.copy(
                correctionSuggestions = suggestions,
                currentSuggestionIndex = nextIndex,
                autocompleteSuggestion = suggestion,
            )
        }
    }

    private val mockNamePool = listOf("김봄", "김보미", "김별", "김봄비", "김보람")

    // 목업 오타교정 후보 — 입력값 제외 후 항상 3개 반환
    private fun getMockCorrectionSuggestions(input: String): List<String> {
        return mockNamePool.filter { it != input }.take(3)
    }

    // 목업 자동완성 후보 — 최근 이체 내역 상위 3개
    private fun getMockAutocompleteSuggestions(): List<String> {
        return mockNamePool.take(3)
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
    val transferAmount: String = "",
    val confirmedCells: List<Set<Int>> = emptyList(),
    val correctionSuggestions: List<String> = emptyList(),
    val currentSuggestionIndex: Int = -1,
    val autocompleteSuggestion: String = "",
)

enum class BrailleMode {
    SERVICE_SELECT,
    TRANSFER_RECIPIENT,
    TYPO_CORRECTION,
    TRANSFER_AMOUNT,
    TRANSFER_CONFIRM,
}
