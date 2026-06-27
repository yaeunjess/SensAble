package com.sensable.app.feature.transfer.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sensable.app.core.common.postTransferBalance
import com.sensable.app.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class TransferConfirmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _autoTriggerBiometric = MutableStateFlow(false)
    val autoTriggerBiometric: StateFlow<Boolean> = _autoTriggerBiometric.asStateFlow()

    init {
        val recipient = URLDecoder.decode(savedStateHandle.get<String>("recipient") ?: "", "UTF-8")
        val amount = savedStateHandle.get<String>("amount") ?: ""
        val formattedAmount = amount.toLongOrNull()?.let { "%,d원".format(it) } ?: "${amount}원"
        val formattedPostBalance = "%,d원".format(postTransferBalance(amount))
        ttsManager.speakWithCompletion(
            "받는 분, $recipient, 금액, $formattedAmount, 이체 후 잔액, $formattedPostBalance 입니다. 송금하시겠습니까? 송금을 원하시면 가운데에 지문인증을 해주세요."
        ) {
            _autoTriggerBiometric.value = true
        }
    }
}
