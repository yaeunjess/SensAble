package com.sensable.app.feature.braille.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finclue.sdk.Finclue
import com.finclue.sdk.api.HistoryPolicy
import com.finclue.sdk.api.PredictionContext
import com.finclue.sdk.api.PredictionMode
import com.finclue.sdk.api.PredictionRequest
import com.finclue.sdk.api.PredictionSelection
import com.sensable.app.core.braille.BrailleDecoder
import com.sensable.app.core.braille.KoreanBrailleStateMachine
import com.sensable.app.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BrailleViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrailleUiState())
    val uiState: StateFlow<BrailleUiState> = _uiState.asStateFlow()
    private val koreanStateMachine = KoreanBrailleStateMachine()

    init {
        viewModelScope.launch { runCatching { Finclue.prewarmPredictions(appContext) } }
    }

    fun onBrailleButtonClick(dot: Int) {
        if (_uiState.value.mode == BrailleMode.AI_RECOMMENDATION) {
            ttsManager.speak("AI 추천 중입니다. 글자 입력으로 다음 추천을 들으세요")
            return
        }
        toggleDot(dot)
    }

    private fun toggleDot(dot: Int) {
        val current = _uiState.value.currentCellDots
        if (dot in current) {
            _uiState.update { it.copy(currentCellDots = current - dot) }
            ttsManager.speak("${dot}번 취소")
        } else {
            _uiState.update { it.copy(currentCellDots = current + dot) }
            ttsManager.speak("${dot}번")
        }
    }

    fun onSwipeRight() {
        val state = _uiState.value
        val dots = state.currentCellDots
        if (state.mode == BrailleMode.AI_RECOMMENDATION) {
            if (dots.isEmpty()) speakNextSuggestion()
            return
        }
        if (dots.isEmpty()) {
            ttsManager.speak("선택한 점이 없습니다")
            return
        }

        if (state.mode == BrailleMode.TRANSFER_RECIPIENT) {
            val committed = koreanStateMachine.process(dots)
            if (!koreanStateMachine.wasLastInputAccepted) {
                ttsManager.speak("현재 위치에 입력할 수 없는 점자입니다")
                _uiState.update { it.copy(currentCellDots = emptySet()) }
                return
            }
            val pending = koreanStateMachine.getPendingDisplay()
            val newText = state.inputText + committed
            ttsManager.speak("글자 입력, ${pending.ifEmpty { committed }}")
            _uiState.update {
                it.copy(
                    currentCellDots = emptySet(),
                    inputText = newText,
                    pendingDisplay = pending,
                    confirmedCells = it.confirmedCells + listOf(dots),
                )
            }
            return
        }

        val decoded = BrailleDecoder.decodeNumber(dots)?.toString()
        if (decoded == null) {
            ttsManager.speak("인식할 수 없는 점자입니다")
            return
        }
        val newText = state.inputText + decoded
        val spoken = if (state.mode == BrailleMode.TRANSFER_AMOUNT) "${newText}원" else decoded
        ttsManager.speak("글자 입력, $spoken")

        _uiState.update {
            it.copy(
                currentCellDots = emptySet(),
                inputText = newText,
                confirmedCells = it.confirmedCells + listOf(dots),
            )
        }
    }

    /** 이름 검색 없이 계좌번호를 바로 입력받는 모드로 진입 (직접 계좌번호 입력 화면 진입 시) */
    fun startAccountNumberEntry() {
        koreanStateMachine.reset()
        ttsManager.speak("받는 사람 이름을 입력하세요")
        _uiState.update {
            BrailleUiState(
                mode = BrailleMode.TRANSFER_RECIPIENT,
                guideMessage = "받는 사람 이름을 입력하세요",
            )
        }
    }

    /** 수취인이 이미 정해진 상태에서 금액만 바로 입력받는 모드로 진입 (송금액 입력 화면 진입 시) */
    fun startAmountEntry(recipientName: String) {
        ttsManager.speak("${recipientName}님에게 얼마를 보낼까요?")
        _uiState.update {
            BrailleUiState(
                mode = BrailleMode.TRANSFER_AMOUNT,
                guideMessage = "얼마를 보낼까요?",
                recipientName = recipientName,
            )
        }
    }

    fun onAiCorrection() {
        val state = _uiState.value
        if (state.mode == BrailleMode.AI_RECOMMENDATION) {
            speakNextSuggestion()
            return
        }
        if (state.mode != BrailleMode.TRANSFER_RECIPIENT) return

        val currentText = state.inputText + state.pendingDisplay
        if (currentText.isBlank()) {
            ttsManager.speak("한 글자 이상 입력한 뒤 AI 보정을 실행해 주세요")
            _uiState.update { it.copy(currentCellDots = emptySet()) }
            return
        }
        if (state.isPredictionLoading) {
            ttsManager.speak("추천을 준비하고 있습니다")
            return
        }

        _uiState.update {
            it.copy(
                mode = BrailleMode.AI_RECOMMENDATION,
                guideMessage = "'$currentText' 추천을 준비하고 있습니다",
                currentCellDots = emptySet(),
                isPredictionLoading = true,
                recommendationBaseText = currentText,
            )
        }
        ttsManager.speak("추천을 준비하고 있습니다")
        viewModelScope.launch {
            val request = PredictionRequest(
                currentText = currentText,
                mode = PredictionMode.PERSONALIZED,
                context = PredictionContext.PERSON_NAME,
                historyPolicy = HistoryPolicy.READ_WRITE,
                limit = 3,
            )
            val personal = runCatching {
                Finclue.requestPersonalPredictions(appContext, request).map { it.text }
            }.getOrDefault(emptyList())
            val generated = runCatching {
                Finclue.requestPredictions(appContext, request).map { it.text }
            }.getOrElse {
                ttsManager.speak("추천을 불러오지 못했습니다. 입력은 계속할 수 있습니다")
                emptyList()
            }
            if (_uiState.value.mode != BrailleMode.AI_RECOMMENDATION) return@launch
            val suggestions = (personal + generated).distinct().take(3)
            _uiState.update {
                it.copy(
                    isPredictionLoading = false,
                    correctionSuggestions = suggestions,
                    currentSuggestionIndex = -1,
                    guideMessage = if (suggestions.isEmpty())
                        "일치하는 이름을 찾지 못했습니다"
                    else "추천 이름 ${suggestions.size}개입니다",
                )
            }
            if (suggestions.isEmpty()) {
                ttsManager.speak("일치하는 이름을 찾지 못했습니다")
            } else {
                speakNextSuggestion()
            }
        }
    }

    private fun speakNextSuggestion() {
        val state = _uiState.value
        if (state.isPredictionLoading) {
            ttsManager.speak("추천을 준비하고 있습니다")
            return
        }
        if (state.correctionSuggestions.isEmpty()) {
            ttsManager.speak("추천 결과가 없습니다")
            return
        }
        val nextIndex = (state.currentSuggestionIndex + 1) % state.correctionSuggestions.size
        val suggestion = state.correctionSuggestions[nextIndex]
        ttsManager.speak(suggestion)
        _uiState.update {
            it.copy(currentSuggestionIndex = nextIndex, autocompleteSuggestion = suggestion)
        }
    }

    fun onDoubleTap() {
        val state = _uiState.value

        when (state.mode) {
            BrailleMode.TRANSFER_RECIPIENT -> {
                val recipient = state.inputText + koreanStateMachine.flush()
                if (recipient.isBlank()) {
                    ttsManager.speak("받는 사람 이름을 입력해 주세요")
                    return
                }
                beginAccountEntry(recipient)
            }
            BrailleMode.AI_RECOMMENDATION -> {
                val recipient = state.autocompleteSuggestion
                if (recipient.isBlank()) {
                    ttsManager.speak("추천 이름을 먼저 선택해 주세요")
                    return
                }
                viewModelScope.launch {
                    runCatching {
                        Finclue.recordPredictionSelection(
                            appContext,
                            PredictionSelection(
                                text = recipient,
                                context = PredictionContext.PERSON_NAME,
                                historyPolicy = HistoryPolicy.READ_WRITE,
                            )
                        )
                    }
                }
                beginAccountEntry(recipient)
            }
            BrailleMode.TRANSFER_ACCOUNT -> {
                if (state.inputText.isBlank()) {
                    ttsManager.speak("계좌번호를 입력해 주세요")
                    return
                }
                val recipient = state.recipientName
                ttsManager.speak("완료, ${recipient}님 계좌번호를 확인했습니다")
                _uiState.update {
                    it.copy(
                        accountEntryCompleted = AccountEntryResult(
                            recipientName = recipient,
                            accountNumber = state.inputText,
                        )
                    )
                }
            }
            BrailleMode.TRANSFER_AMOUNT -> {
                val formattedAmount = "%,d원".format(state.inputText.toLongOrNull() ?: 0L)
                ttsManager.speak("완료, ${formattedAmount} 입력 확인되었습니다.")
                _uiState.update { it.copy(amountEntryCompleted = state.inputText) }
            }
        }
    }

    private fun beginAccountEntry(recipient: String) {
        koreanStateMachine.reset()
        ttsManager.speak("${recipient}님의 계좌번호를 입력하세요")
        _uiState.update {
            BrailleUiState(
                mode = BrailleMode.TRANSFER_ACCOUNT,
                guideMessage = "계좌번호를 입력하세요",
                recipientName = recipient,
            )
        }
    }

    // true를 반환하면 호출자가 바텀시트를 닫아야 함
    fun onSwipeLeft(): Boolean {
        val state = _uiState.value

        if (state.mode == BrailleMode.AI_RECOMMENDATION) {
            ttsManager.speak("AI 추천을 닫고 이름 입력으로 돌아갑니다")
            _uiState.update {
                it.copy(
                    mode = BrailleMode.TRANSFER_RECIPIENT,
                    guideMessage = "받는 사람 이름을 입력하세요",
                    correctionSuggestions = emptyList(),
                    currentSuggestionIndex = -1,
                    autocompleteSuggestion = "",
                    isPredictionLoading = false,
                    currentCellDots = emptySet(),
                )
            }
            return false
        }

        // 1. 현재 셀에 점이 선택된 상태 → 셀만 초기화
        if (state.currentCellDots.isNotEmpty()) {
            ttsManager.speak("삭제, 선택한 점을 모두 취소했습니다")
            _uiState.update { it.copy(currentCellDots = emptySet()) }
            return false
        }

        // 2. 확정된 입력(셀)이 있으면 → 마지막 셀 하나 되돌리기
        val cells = state.confirmedCells
        if (cells.isNotEmpty()) {
            val newCells = cells.dropLast(1)
            val (rebuiltText, rebuiltPending) = if (state.mode == BrailleMode.TRANSFER_RECIPIENT) {
                replayNameCells(newCells)
            } else {
                newCells.joinToString("") { BrailleDecoder.decodeNumber(it)?.toString() ?: "" } to ""
            }
            ttsManager.speak(
                if (rebuiltText.isEmpty()) "삭제, 모두 지워졌습니다"
                else "삭제, 현재 입력은 $rebuiltText"
            )
            _uiState.update {
                it.copy(
                    inputText = rebuiltText,
                    pendingDisplay = rebuiltPending,
                    currentCellDots = emptySet(),
                    confirmedCells = newCells,
                )
            }
            return false
        }

        // 3. 더 되돌아갈 단계가 없으면 → 바텀시트를 닫는다
        ttsManager.speak("삭제할 내용이 없습니다")
        return true
    }

    private fun replayNameCells(cells: List<Set<Int>>): Pair<String, String> {
        koreanStateMachine.reset()
        var text = ""
        cells.forEach { text += koreanStateMachine.process(it) }
        return text to koreanStateMachine.getPendingDisplay()
    }

    fun reset() {
        _uiState.update { BrailleUiState() }
    }
}

data class BrailleUiState(
    val mode: BrailleMode = BrailleMode.TRANSFER_RECIPIENT,
    val guideMessage: String = "",
    val currentCellDots: Set<Int> = emptySet(),
    val inputText: String = "",
    val pendingDisplay: String = "",
    val recipientName: String = "",
    val confirmedCells: List<Set<Int>> = emptyList(),
    val correctionSuggestions: List<String> = emptyList(),
    val currentSuggestionIndex: Int = -1,
    val autocompleteSuggestion: String = "",
    val recommendationBaseText: String = "",
    val isPredictionLoading: Boolean = false,
    val accountEntryCompleted: AccountEntryResult? = null,
    val amountEntryCompleted: String? = null,
)

/** 계좌번호 직접입력 화면에서 바텀시트로 입력을 마쳤을 때 상위 화면에 전달할 결과 */
data class AccountEntryResult(
    val recipientName: String,
    val accountNumber: String,
)

enum class BrailleMode {
    TRANSFER_RECIPIENT,
    AI_RECOMMENDATION,
    TRANSFER_ACCOUNT,
    TRANSFER_AMOUNT,
}
