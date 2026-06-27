package com.sensable.app.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                tts?.setPitch(0.85f)
                tts?.setSpeechRate(0.88f)
                isReady = true
                pendingText?.let { speak(it) }
                pendingText = null
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            pendingText = text
        }
    }

    /** 현재 발화 중인 음성 뒤에 이어서 재생 (버튼 번호 → 안내문 순서 보장) */
    fun speakQueued(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        } else {
            pendingText = text
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
