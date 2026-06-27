package com.sensable.app.core.designsystem.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensable.app.ui.theme.KakaoYellow
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

@Composable
fun BrailleGrid(
    onButtonClick: (dot: Int) -> Unit,
    onSwipeRight: () -> Unit,
    pressedDots: Set<Int> = emptySet(),
    onDoubleTap: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val swipe = remember { SwipeState() }

    var columnModifier = modifier
        .fillMaxWidth()
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
                            swipe.totalDragY < -80f -> { swipe.hasFired = true; onSwipeUp?.invoke() }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { col ->
                    val dotNumber = row + (1 - col) * 3 + 1
                    BrailleButton(
                        label = "$dotNumber",
                        isPressed = dotNumber in pressedDots,
                        onClick = { onButtonClick(dotNumber) },
                        modifier = Modifier.weight(1f)
                    )
                    // 왼쪽 버튼(col=0) 다음에 넓은 중앙 공간 — 더블탭 영역
                    if (col == 0) Spacer(modifier = Modifier.weight(0.6f))
                }
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
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun BrailleButton(
    label: String,
    isPressed: Boolean,
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
        shape = CircleShape,
        modifier = modifier.aspectRatio(1f),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) KakaoYellow else Color(0xFFDDDDDD),
            contentColor = if (active) Color.Black else Color.Gray
        )
    ) {
        Text(text = label, fontSize = 36.sp)
    }
}