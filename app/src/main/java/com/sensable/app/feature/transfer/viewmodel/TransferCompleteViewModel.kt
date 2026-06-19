package com.sensable.app.feature.transfer.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sensable.app.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class TransferCompleteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ttsManager: TtsManager
) : ViewModel() {

    init {
        val recipient = URLDecoder.decode(savedStateHandle.get<String>("recipient") ?: "", "UTF-8")
        val amount = savedStateHandle.get<String>("amount") ?: ""
        val formattedAmount = amount.toLongOrNull()?.let { "%,d원".format(it) } ?: "${amount}원"
        ttsManager.speak("${recipient}님에게 ${formattedAmount}원을 보냈습니다.")
    }
}
