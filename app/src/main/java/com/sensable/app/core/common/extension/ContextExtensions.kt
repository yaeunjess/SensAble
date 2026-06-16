package com.sensable.app.core.common.extension

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

fun Context.createTts(onReady: (TextToSpeech) -> Unit): TextToSpeech {
    var tts: TextToSpeech? = null
    tts = TextToSpeech(this) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            tts?.let(onReady)
        }
    }
    return tts
}