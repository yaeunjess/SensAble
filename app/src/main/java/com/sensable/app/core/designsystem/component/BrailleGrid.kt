package com.sensable.app.core.designsystem.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensable.app.ui.theme.SensableBlue
import com.sensable.app.ui.theme.SensableBlueContent
import com.sensable.app.ui.theme.SensableDarkButtonIdle
import com.sensable.app.ui.theme.SensableDarkButtonIdleText
import com.sensable.app.ui.theme.SensableTheme

private fun vibrateTap(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(80L, 200))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(80L)
    }
}

// 왼쪽 끝에 붙는 버튼: 왼쪽 직각 + 오른쪽 36dp 라운드 — 점 4, 5, 6
private val LeftBrailleButtonShape = RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp)

// 오른쪽 끝에 붙는 버튼: 왼쪽 36dp 라운드 + 오른쪽 직각 — 점 1, 2, 3
private val RightBrailleButtonShape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp)

/**
 * 3행 2열 점자 인터페이스 그리드 — 쓰기 방향 기준.
 *
 * 점자는 종이 뒤에서 찍고 앞에서 읽으므로 입력 UI는 쓰기 방향(좌우 반전)으로 배치.
 *
 *   화면(쓰기 방향):    표준 점자(읽기 방향):
 *   [4] [1]             [1] [4]
 *   [5] [2]      vs     [2] [5]
 *   [6] [3]             [3] [6]
 */
private class SwipeState {
    var totalDragX = 0f
    var totalDragY = 0f
    var hasFired = false
}

private class HoverInputState {
    var size = Size.Zero
}

@Composable
private fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    var enabled by remember(manager) {
        mutableStateOf(manager.isEnabled && manager.isTouchExplorationEnabled)
    }

    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener {
            enabled = manager.isEnabled && it
        }
        manager.addTouchExplorationStateChangeListener(listener)
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BrailleGrid(
    onButtonClick: (dot: Int) -> Unit,
    onSwipeRight: () -> Unit,
    pressedDots: Set<Int> = emptySet(),
    onDoubleTap: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onAiCorrection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val swipe = remember { SwipeState() }
    val hover = remember { HoverInputState() }
    val touchExplorationEnabled = rememberTouchExplorationEnabled()
    val context = LocalContext.current

    fun dotAt(x: Float, y: Float): Int? {
        if (hover.size.width <= 0f || hover.size.height <= 0f) return null
        if (x < 0f || x >= hover.size.width || y < 0f || y >= hover.size.height) return null
        val actionAreaHeight = 72f * context.resources.displayMetrics.density
        val dotAreaHeight = hover.size.height - actionAreaHeight
        if (dotAreaHeight <= 0f || y >= dotAreaHeight) return null
        val column = if (x < hover.size.width / 2f) 0 else 1
        val row = ((y / dotAreaHeight) * 3).toInt().coerceIn(0, 2)
        return row + (1 - column) * 3 + 1
    }

    fun actionRowAt(x: Float, y: Float): Int? {
        if (hover.size.width <= 0f || hover.size.height <= 0f) return null
        if (x < 0f || x >= hover.size.width || y < 0f || y >= hover.size.height) return null
        val actionHeight = 64f * context.resources.displayMetrics.density
        if (y < hover.size.height - actionHeight) return null
        val horizontalRatio = x / hover.size.width
        return when {
            horizontalRatio < 0.25f -> 0
            horizontalRatio < 0.75f -> 1
            else -> 2
        }
    }

    fun runCenterAction(row: Int) {
        vibrateTap(context)
        when (row) {
            0 -> onSwipeLeft?.invoke()
            1 -> onSwipeRight()
            2 -> onDoubleTap?.invoke()
        }
    }

    var columnModifier = modifier
        .fillMaxWidth()
        .onSizeChanged { hover.size = Size(it.width.toFloat(), it.height.toFloat()) }
        .then(
            if (touchExplorationEnabled) {
                Modifier
                    .clearAndSetSemantics {
                        customActions = buildList {
                            add(
                            CustomAccessibilityAction("선택한 점자 입력") {
                                vibrateTap(context)
                                onSwipeRight()
                                true
                            })
                            add(
                            CustomAccessibilityAction("삭제") {
                                vibrateTap(context)
                                onSwipeLeft?.invoke()
                                true
                            })
                            onAiCorrection?.let { requestAiCorrection ->
                                add(
                                    CustomAccessibilityAction("AI 입력 보정") {
                                        vibrateTap(context)
                                        requestAiCorrection()
                                        true
                                    }
                                )
                            }
                            add(
                            CustomAccessibilityAction("전체 입력 완료") {
                                vibrateTap(context)
                                onDoubleTap?.invoke()
                                true
                            })
                        }
                    }
                    .pointerInteropFilter { event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_HOVER_ENTER -> {
                                dotAt(event.x, event.y)?.let {
                                    vibrateTap(context)
                                    onButtonClick(it)
                                } ?: actionRowAt(event.x, event.y)?.let(::runCenterAction)
                            }

                            MotionEvent.ACTION_HOVER_MOVE,
                            MotionEvent.ACTION_HOVER_EXIT -> Unit

                            else -> return@pointerInteropFilter false
                        }
                        true
                    }
            } else {
                Modifier
            }
        )
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    swipe.totalDragX = 0f
                    swipe.totalDragY = 0f
                    swipe.hasFired = false
                },
                onDrag = { _, dragAmount ->
                    swipe.totalDragX += dragAmount.x
                    swipe.totalDragY += dragAmount.y
                    if (!swipe.hasFired) {
                        when {
                            swipe.totalDragX > 80f  -> { swipe.hasFired = true; onSwipeRight() }
                            swipe.totalDragX < -80f -> { swipe.hasFired = true; onSwipeLeft?.invoke() }
                        }
                    }
                }
            )
        }

    if (onDoubleTap != null) {
        columnModifier = columnModifier.pointerInput("doubleTap") {
            detectTapGestures(onDoubleTap = { onDoubleTap() })
        }
    }

    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) { col ->
                    val dotNumber = row + (1 - col) * 3 + 1
                    val isLeftSide = (col == 0)
                    BrailleButton(
                        label = "$dotNumber",
                        isPressed = dotNumber in pressedDots,
                        isLeftSide = isLeftSide,
                        onClick = { onButtonClick(dotNumber) },
                        modifier = Modifier.weight(1f)
                    )
                    // 왼쪽 버튼(col=0) 다음에 넓은 중앙 공간 — 더블탭 영역
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { runCenterAction(0) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SensableDarkButtonIdle,
                    contentColor = SensableDarkButtonIdleText
                )
            ) {
                Text("삭제", fontSize = 14.sp)
            }
            Button(
                onClick = { runCenterAction(1) },
                modifier = Modifier.weight(2f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SensableBlue,
                    contentColor = SensableBlueContent
                )
            ) {
                Text("글자 입력", fontSize = 14.sp)
            }
            Button(
                onClick = { runCenterAction(2) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SensableDarkButtonIdle,
                    contentColor = SensableDarkButtonIdleText
                )
            ) {
                Text("완료", fontSize = 14.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "점자 그리드")
@Composable
private fun BrailleGridPreview() {
    SensableTheme {
        BrailleGrid(
            onButtonClick = { _ -> },
            onSwipeRight = {},
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
private fun BrailleButton(
    label: String,
    isPressed: Boolean,
    isLeftSide: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isTouching by interactionSource.collectIsPressedAsState()
    val active = isPressed || isTouching

    Button(
        onClick = {
            vibrateTap(context)
            onClick()
        },
        shape = if (isLeftSide) LeftBrailleButtonShape else RightBrailleButtonShape,
        modifier = modifier.fillMaxHeight(),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = (if (active) SensableBlue else SensableDarkButtonIdle).copy(alpha = 0.9f),
            contentColor = if (active) SensableBlueContent else SensableDarkButtonIdleText
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            focusedElevation = 6.dp,
            hoveredElevation = 8.dp
        )
    ) {
        Text(
            text = label,
            fontSize = 36.sp,
            textAlign = TextAlign.Center
        )
    }
}
