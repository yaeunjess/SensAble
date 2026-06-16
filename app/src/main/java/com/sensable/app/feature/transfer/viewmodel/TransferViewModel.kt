package com.sensable.app.feature.transfer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensable.app.domain.model.TransferInfo
import com.sensable.app.domain.usecase.transfer.TransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferUseCase: TransferUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private var transferInfo = TransferInfo()

    fun onBrailleInput(row: Int, col: Int) {
        // TODO: 점자 입력 → 문자 변환 로직 구현
    }

    fun onConfirm() {
        when (_uiState.value.step) {
            TransferStep.RECIPIENT -> _uiState.update {
                it.copy(
                    step = TransferStep.AMOUNT,
                    guideMessage = "${transferInfo.recipientName}님께 얼마를 보낼까요?",
                    inputDisplay = ""
                )
            }
            TransferStep.AMOUNT -> _uiState.update {
                it.copy(
                    step = TransferStep.CONFIRM,
                    guideMessage = "${transferInfo.recipientName}님께 ${transferInfo.amount}원 송금하는게 맞습니까?"
                )
            }
            TransferStep.CONFIRM -> processTransfer()
            TransferStep.DONE -> {
                // 이미 완료된 상태이므로 추가 동작 없음 또는 초기화/종료 처리
            }
        }
    }

    private fun processTransfer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transferUseCase(transferInfo)
            _uiState.update { it.copy(isLoading = false, step = TransferStep.DONE) }
        }
    }
}

data class TransferUiState(
    val step: TransferStep = TransferStep.RECIPIENT,
    val guideMessage: String = "누구에게 보낼까요?",
    val inputDisplay: String = "",
    val isLoading: Boolean = false
)

enum class TransferStep {
    RECIPIENT, AMOUNT, CONFIRM, DONE
}
