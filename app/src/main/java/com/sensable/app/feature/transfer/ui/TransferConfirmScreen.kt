package com.sensable.app.feature.transfer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.core.common.postTransferBalance
import com.sensable.app.core.navigation.Screen
import com.sensable.app.feature.transfer.viewmodel.TransferConfirmViewModel
import com.sensable.app.ui.theme.KakaoYellow
import com.sensable.app.ui.theme.SensableTheme
import kotlinx.coroutines.delay

private enum class FingerprintState { IDLE, SCANNING, SUCCESS }

@Composable
fun TransferConfirmScreen(
    navController: NavController,
    recipient: String = "이지영",
    amount: String = "50000",
    viewModel: TransferConfirmViewModel = hiltViewModel()
) {
    val formattedAmount = formatAmount(amount)
    val showFingerprintOverlay by viewModel.showFingerprintSheet.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                Button(
                    onClick = { viewModel.onAuthButtonClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KakaoYellow,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "인증하기",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${recipient}님께",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00897B)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        InfoRow(label = "보내는 분", value = "김예은")
                        InfoRow(label = "받는 분", value = recipient)
                        InfoRow(label = "금액", value = formattedAmount, valueColor = Color(0xFF00897B))
                        InfoRow(
                            label = "이체 후 잔액",
                            value = "%,d원".format(postTransferBalance(amount)),
                            showDivider = false
                        )
                    }
                }
            }
        }

        // 딤 오버레이 + 지문 인증 카드
        AnimatedVisibility(
            visible = showFingerprintOverlay,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            FingerprintAuthOverlay(
                recipient = recipient,
                formattedAmount = formattedAmount,
                onDismiss = { viewModel.onSheetDismissed() },
                onAuthSuccess = {
                    viewModel.onSheetDismissed()
                    navController.navigate(Screen.TransferComplete.createRoute(recipient, amount))
                }
            )
        }
    }
}

@Composable
private fun FingerprintAuthOverlay(
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

    // 딤 배경 — 탭하면 취소
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = fingerprintState == FingerprintState.IDLE
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // 인증 카드 — 탭 이벤트가 배경까지 전파되지 않도록 차단
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(tween(250), initialScale = 0.85f) + fadeIn(tween(250)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 카드 내부 탭은 배경으로 전파 차단 */ },
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

                    // 지문 아이콘 영역
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(136.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = fingerprintState == FingerprintState.IDLE
                            ) {
                                fingerprintState = FingerprintState.SCANNING
                            }
                    ) {
                        // 스캔 중 펄스 원
                        if (fingerprintState == FingerprintState.SCANNING) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(pulseScale)
                                    .background(Color(0x1A1976D2), CircleShape)
                            )
                        }
                        // 성공 시 연한 초록 원
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
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
    if (showDivider) {
        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
    }
}

@Preview(showBackground = true, name = "송금 전 확인")
@Composable
private fun TransferConfirmScreenPreview() {
    SensableTheme {
        TransferConfirmScreen(navController = rememberNavController())
    }
}
