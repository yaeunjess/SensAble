package com.finclue.sdk.internal

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.finclue.sdk.api.FieldSpec
import com.finclue.sdk.api.FieldType
import com.finclue.sdk.api.FlowResult
import com.finclue.sdk.storage.FlowSessionEntity
import com.finclue.sdk.storage.LocalDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

internal class FinclueFlowActivity : Activity(), TextToSpeech.OnInitListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var flowId: String
    private lateinit var pendingFlow: PendingFlow
    private lateinit var dataStore: LocalDataStore
    private val sessionId = CompletableDeferred<Long>()
    private var fieldIndex = 0
    private var fieldStartedAt = 0L
    private var correctionCount = 0
    private val values = linkedMapOf<String, String>()
    private val currentDots = linkedSetOf<Int>()
    private val dotButtons = mutableMapOf<Int, Button>()
    private lateinit var promptView: TextView
    private lateinit var valueView: TextView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var delivered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flowId = intent.getStringExtra(EXTRA_FLOW_ID).orEmpty()
        val flow = PendingFlowStore.get(flowId)
        if (flow == null) {
            finish()
            return
        }
        pendingFlow = flow
        dataStore = LocalDataStore(applicationContext)
        scope.launch { sessionId.complete(dataStore.startSession()) }
        tts = TextToSpeech(this, this)
        setContentView(createContentView())
        showCurrentField()
    }

    private fun createContentView(): View {
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((20 * density).toInt(), (28 * density).toInt(), (20 * density).toInt(), (24 * density).toInt())
            setBackgroundColor(Color.rgb(17, 24, 39))
        }

        promptView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        }
        valueView = TextView(this).apply {
            setTextColor(Color.rgb(125, 211, 252))
            textSize = 28f
            gravity = Gravity.CENTER
        }
        root.addView(promptView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.65f))
        root.addView(valueView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.35f))

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // Writing direction: [4,1] / [5,2] / [6,3]. Decoder receives standard dot numbers.
        listOf(4 to 1, 5 to 2, 6 to 3).forEach { (left, right) ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(createDotButton(left), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = (8 * density).toInt() })
            row.addView(createDotButton(right), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = (8 * density).toInt() })
            grid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { setMargins(0, (6 * density).toInt(), 0, (6 * density).toInt()) })
        }
        root.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 3f))

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onDoubleTap(e: MotionEvent): Boolean { confirmField(); return true }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || abs(e2.x - e1.x) < 100 * density) return false
                if (e2.x > e1.x) confirmCell() else deleteLastCharacter()
                return true
            }
        })
        root.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
        return root
    }

    private fun createDotButton(dot: Int) = Button(this).apply {
        text = dot.toString()
        textSize = 32f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(51, 65, 85))
        contentDescription = "$dot 점"
        setOnClickListener { toggleDot(dot) }
        dotButtons[dot] = this
    }

    private fun toggleDot(dot: Int) {
        if (!currentDots.add(dot)) currentDots.remove(dot)
        dotButtons[dot]?.alpha = if (dot in currentDots) 0.55f else 1f
        vibrate()
        speak("$dot 점")
    }

    private fun confirmCell() {
        val field = currentField()
        if (currentDots.isEmpty()) return
        val decoded = when (field.type) {
            FieldType.ACCOUNT, FieldType.AMOUNT, FieldType.PIN -> BrailleNumberDecoder.decode(currentDots)
        }
        if (decoded == null) {
            correctionCount++
            speak("인식할 수 없는 점자입니다")
        } else {
            values[field.key] = values[field.key].orEmpty() + decoded
            updateValueView(field)
            speak(decoded.toString())
        }
        clearDots()
    }

    private fun deleteLastCharacter() {
        if (currentDots.isNotEmpty()) {
            clearDots()
            speak("입력을 취소했습니다")
            return
        }
        val field = currentField()
        val value = values[field.key].orEmpty()
        if (value.isNotEmpty()) {
            values[field.key] = value.dropLast(1)
            correctionCount++
            updateValueView(field)
            speak("한 글자를 지웠습니다")
        }
    }

    private fun confirmField() {
        if (currentDots.isNotEmpty()) confirmCell()
        val field = currentField()
        val value = values[field.key].orEmpty()
        if (value.isBlank()) {
            speak("입력값이 없습니다")
            return
        }
        val duration = System.currentTimeMillis() - fieldStartedAt
        scope.launch {
            dataStore.recordField(
                sessionId = sessionId.await(),
                fieldKey = field.key,
                fieldType = field.type.name,
                inputLength = value.length,
                correctionCount = correctionCount,
                durationMillis = duration,
            )
        }
        if (fieldIndex == pendingFlow.spec.fields.lastIndex) {
            complete(FlowResult.Success(values.toMap()), FlowSessionEntity.STATUS_COMPLETED)
        } else {
            fieldIndex++
            correctionCount = 0
            showCurrentField()
        }
    }

    private fun showCurrentField() {
        val field = currentField()
        fieldStartedAt = System.currentTimeMillis()
        promptView.text = field.prompt
        updateValueView(field)
        speak(field.prompt)
    }

    private fun updateValueView(field: FieldSpec) {
        val raw = values[field.key].orEmpty()
        valueView.text = if (field.type == FieldType.PIN) "●".repeat(raw.length) else raw
    }

    private fun currentField() = pendingFlow.spec.fields[fieldIndex]

    private fun clearDots() {
        currentDots.clear()
        dotButtons.values.forEach { it.alpha = 1f }
    }

    override fun onBackPressed() {
        complete(FlowResult.Cancelled, FlowSessionEntity.STATUS_CANCELLED)
    }

    private fun complete(result: FlowResult, status: String) {
        if (delivered) return
        delivered = true
        PendingFlowStore.remove(flowId)?.callback?.invoke(result)
        scope.launch {
            dataStore.finishSession(sessionId.await(), status)
        }
        finish()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS && tts?.setLanguage(Locale.KOREAN) != TextToSpeech.LANG_MISSING_DATA
        if (ttsReady && ::promptView.isInitialized) speak(promptView.text.toString())
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "finclue")
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator?.vibrate(60)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        if (isFinishing && !delivered && ::pendingFlow.isInitialized) {
            complete(FlowResult.Error("FIN:CLUE flow was destroyed before completion."), FlowSessionEntity.STATUS_ERROR)
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_FLOW_ID = "com.finclue.sdk.extra.FLOW_ID"
    }
}
