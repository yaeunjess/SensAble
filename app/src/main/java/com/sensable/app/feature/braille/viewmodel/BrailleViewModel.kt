package com.sensable.app.feature.braille.viewmodel

import androidx.lifecycle.ViewModel
import com.sensable.app.core.braille.BrailleDecoder
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

    fun onBrailleButtonClick(dot: Int) {
        ttsManager.speak(dot.toString())
        toggleDot(dot)
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
        val dots = state.currentCellDots
        if (dots.isEmpty()) return

        val decoded = BrailleDecoder.decodeNumber(dots)?.toString() ?: return

        val newText = state.inputText + decoded
        val spoken = if (state.mode == BrailleMode.TRANSFER_AMOUNT) "${newText}원" else decoded
        ttsManager.speak(spoken)

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
        ttsManager.speak("계좌번호를 입력하세요")
        _uiState.update {
            BrailleUiState(
                mode = BrailleMode.TRANSFER_RECIPIENT,
                guideMessage = "계좌번호를 입력하세요",
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

    fun onDoubleTap() {
        val state = _uiState.value

        when (state.mode) {
            BrailleMode.TRANSFER_RECIPIENT -> {
                val recipient = "이지영"
                ttsManager.speak("${recipient}님 계좌로 확인되었습니다.")
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
                ttsManager.speak("${formattedAmount} 입력 확인되었습니다.")
                _uiState.update { it.copy(amountEntryCompleted = state.inputText) }
            }
        }
    }

    // true를 반환하면 호출자가 바텀시트를 닫아야 함
    fun onSwipeLeft(): Boolean {
        val state = _uiState.value

        // 1. 현재 셀에 점이 선택된 상태 → 셀만 초기화
        if (state.currentCellDots.isNotEmpty()) {
            ttsManager.speak("취소")
            _uiState.update { it.copy(currentCellDots = emptySet()) }
            return false
        }

        // 2. 확정된 입력(셀)이 있으면 → 마지막 셀 하나 되돌리기
        val cells = state.confirmedCells
        if (cells.isNotEmpty()) {
            val newCells = cells.dropLast(1)
            val rebuiltText = newCells.joinToString("") { BrailleDecoder.decodeNumber(it)?.toString() ?: "" }
            ttsManager.speak(rebuiltText.ifEmpty { "모두 지워졌습니다" })
            _uiState.update {
                it.copy(
                    inputText = rebuiltText,
                    currentCellDots = emptySet(),
                    confirmedCells = newCells,
                )
            }
            return false
        }

        // 3. 더 되돌아갈 단계가 없으면 → 바텀시트를 닫는다
        return true
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
    val recipientName: String = "",
    val confirmedCells: List<Set<Int>> = emptyList(),
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
    TRANSFER_AMOUNT,
}
