package com.sensable.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class FingerprintState { IDLE, SCANNING, SUCCESS }

/** 지문 인증 진행 화면 — 이체 확인 후 전체 화면 위에 겹쳐서 표시되는 오버레이 */
@Composable
fun FingerprintAuthOverlay(
    recipient: String,
    formattedAmount: String,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    var fingerprintState by remember { mutableStateOf(FingerprintState.IDLE) }

    val iconColor by animateColorAsState(
        targetValue = when (fingerprintState) {
            FingerprintState.IDLE -> Color(0xFFBDBDBD)
            FingerprintState.SCANNING -> Color(0xFF1976D2)
            FingerprintState.SUCCESS -> Color(0xFF43A047)
        },
        animationSpec = tween(300),
        label = "iconColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale"
    )

    LaunchedEffect(fingerprintState) {
        when (fingerprintState) {
            FingerprintState.SCANNING -> {
                delay(1600)
                fingerprintState = FingerprintState.SUCCESS
            }
            FingerprintState.SUCCESS -> {
                delay(800)
                onAuthSuccess()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = fingerprintState == FingerprintState.IDLE
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "지문 인증",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF212121)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${recipient}님께\n$formattedAmount 송금",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )

                    Spacer(Modifier.height(36.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(136.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = fingerprintState == FingerprintState.IDLE
                            ) { fingerprintState = FingerprintState.SCANNING }
                    ) {
                        if (fingerprintState == FingerprintState.SCANNING) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(pulseScale)
                                    .background(Color(0x1A1976D2), CircleShape)
                            )
                        }
                        if (fingerprintState == FingerprintState.SUCCESS) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(Color(0x1A43A047), CircleShape)
                            )
                        }
                        Icon(
                            imageVector = if (fingerprintState == FingerprintState.SUCCESS)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(76.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = when (fingerprintState) {
                            FingerprintState.IDLE -> "손가락을 올려주세요"
                            FingerprintState.SCANNING -> "지문 인식 중..."
                            FingerprintState.SUCCESS -> "인증 완료"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = when (fingerprintState) {
                            FingerprintState.IDLE -> Color(0xFF616161)
                            FingerprintState.SCANNING -> Color(0xFF1976D2)
                            FingerprintState.SUCCESS -> Color(0xFF43A047)
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(28.dp))

                    if (fingerprintState == FingerprintState.IDLE) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "취소",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    } else {
                        Spacer(Modifier.height(40.dp))
                    }
                }
        }
    }
}
