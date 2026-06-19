package com.sensable.app.feature.transfer.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sensable.app.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class TransferConfirmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ttsManager: TtsManager
) : ViewModel() {

    init {
        val recipient = URLDecoder.decode(savedStateHandle.get<String>("recipient") ?: "", "UTF-8")
        val amount = savedStateHandle.get<String>("amount") ?: ""
        val formattedAmount = amount.toLongOrNull()?.let { "%,d원".format(it) } ?: "${amount}원"
        ttsManager.speak("받는 분, $recipient, 금액, $formattedAmount, 이체 후 잔액, 1,500,000원 입니다. 송금하시겠습니까? 송금을 원하시면 가운데에 지문인증을 해주세요.")
    }
}
