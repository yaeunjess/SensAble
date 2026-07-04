package com.sensable.app.feature.braille.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sensable.app.core.designsystem.component.BrailleGrid
import com.sensable.app.core.navigation.Screen
import com.sensable.app.feature.braille.viewmodel.BrailleMode
import com.sensable.app.feature.braille.viewmodel.BrailleViewModel
import com.sensable.app.ui.theme.SensableBlue
import com.sensable.app.ui.theme.SensableDarkOnSurface
import com.sensable.app.ui.theme.SensableDarkSubtext
import com.sensable.app.ui.theme.SensableDarkSurface
import com.sensable.app.ui.theme.SensableTheme
import kotlinx.coroutines.delay

private enum class FingerprintState { IDLE, SCANNING, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrailleBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    viewModel: BrailleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false }
        ),
        containerColor = SensableDarkSurface,
        contentColor = SensableDarkOnSurface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            BrailleBottomSheetContent(
                guideMessage = uiState.guideMessage,
                currentCellDots = uiState.currentCellDots,
                inputText = uiState.inputText,
                pendingDisplay = uiState.pendingDisplay,
                recipientName = uiState.recipientName,
                autocompleteSuggestion = uiState.autocompleteSuggestion,
                confirmRecipient = uiState.confirmRecipient,
                confirmAmount = uiState.confirmAmount,
                confirmBalance = uiState.confirmBalance,
                mode = uiState.mode,
                onButtonClick = { dot -> viewModel.onBrailleButtonClick(dot) },
                onSwipeRight = { viewModel.onSwipeRight() },
                onSwipeLeft = { if (viewModel.onSwipeLeft()) onDismiss() },
                onSwipeUp = { viewModel.onSwipeUp() },
                onDoubleTap = { viewModel.onDoubleTap() },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(vertical = 16.dp)
            )

            if (uiState.showFingerprintOverlay) {
                FingerprintAuthOverlay(
                    recipient = uiState.confirmRecipient,
                    formattedAmount = uiState.confirmAmount,
                    onDismiss = { viewModel.onFingerprintDismissed() },
                    onAuthSuccess = {
                        viewModel.onFingerprintDismissed()
                        navController.navigate(
                            Screen.TransferComplete.createRoute(
                                uiState.confirmRecipient,
                                uiState.transferAmount
                            )
                        )
                        onDismiss()
                    }
                )
            }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
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

@Composable
internal fun BrailleBottomSheetContent(
    guideMessage: String,
    currentCellDots: Set<Int>,
    inputText: String,
    pendingDisplay: String,
    recipientName: String,
    mode: BrailleMode,
    onButtonClick: (dot: Int) -> Unit,
    onSwipeRight: () -> Unit,
    onDoubleTap: () -> Unit,
    autocompleteSuggestion: String = "",
    confirmRecipient: String = "",
    confirmAmount: String = "",
    confirmBalance: String = "",
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (mode == BrailleMode.TRANSFER_CONFIRM)
                        Modifier.heightIn(min = 200.dp)
                    else
                        Modifier.height(140.dp)
                )
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() })
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val guideAnnotated = when {
                mode == BrailleMode.TRANSFER_CONFIRM && confirmRecipient.isNotEmpty() -> buildAnnotatedString {
                    withStyle(SpanStyle(color = SensableBlue, fontWeight = FontWeight.SemiBold)) { append(confirmRecipient) }
                    withStyle(SpanStyle(color = SensableDarkOnSurface)) { append("님에게\n") }
                    withStyle(SpanStyle(color = SensableBlue, fontWeight = FontWeight.SemiBold)) { append(confirmAmount) }
                    withStyle(SpanStyle(color = SensableDarkOnSurface)) { append("을 보내시겠습니까?\n이체 후 잔액은 ") }
                    withStyle(SpanStyle(color = SensableBlue, fontWeight = FontWeight.SemiBold)) { append(confirmBalance) }
                    withStyle(SpanStyle(color = SensableDarkOnSurface)) { append("입니다.") }
                }
                mode == BrailleMode.TRANSFER_AMOUNT && recipientName.isNotEmpty() -> buildAnnotatedString {
                    withStyle(SpanStyle(color = SensableBlue, fontWeight = FontWeight.SemiBold)) {
                        append(recipientName)
                    }
                    withStyle(SpanStyle(color = SensableDarkOnSurface)) {
                        append("님에게 얼마를 보낼까요?")
                    }
                }
                else -> buildAnnotatedString {
                    withStyle(SpanStyle(color = SensableDarkOnSurface)) { append(guideMessage) }
                }
            }

            Text(
                text = guideAnnotated,
                style = MaterialTheme.typography.titleLarge,
                fontSize = if (mode == BrailleMode.TRANSFER_CONFIRM) 22.sp else MaterialTheme.typography.titleLarge.fontSize,
                lineHeight = if (mode == BrailleMode.TRANSFER_CONFIRM) 28.sp else MaterialTheme.typography.titleLarge.lineHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            val displayAnnotated = when {
                autocompleteSuggestion.isNotEmpty() -> buildAnnotatedString {
                    withStyle(SpanStyle(color = SensableBlue, fontWeight = FontWeight.Bold)) {
                        append(autocompleteSuggestion)
                    }
                }
                inputText.isNotEmpty() || pendingDisplay.isNotEmpty() -> buildAnnotatedString {
                    val displayInput = if (mode == BrailleMode.TRANSFER_AMOUNT && inputText.isNotEmpty()) {
                        "%,d원".format(inputText.toLongOrNull() ?: 0L)
                    } else inputText
                    withStyle(SpanStyle(color = SensableDarkOnSurface, fontWeight = FontWeight.Bold)) {
                        append(displayInput)
                    }
                    if (pendingDisplay.isNotEmpty()) {
                        withStyle(SpanStyle(color = SensableDarkSubtext)) {
                            if (inputText.isNotEmpty()) append(" | ")
                            append(pendingDisplay)
                        }
                    }
                }
                mode == BrailleMode.TYPO_CORRECTION && recipientName.isNotEmpty() -> buildAnnotatedString {
                    withStyle(SpanStyle(color = SensableDarkOnSurface, fontWeight = FontWeight.Bold)) {
                        append(recipientName)
                    }
                }
                else -> null
            }
            if (displayAnnotated != null) {
                Text(
                    text = displayAnnotated,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        BrailleGrid(
            onButtonClick = onButtonClick,
            onSwipeRight = onSwipeRight,
            onSwipeLeft = onSwipeLeft,
            onSwipeUp = onSwipeUp,
            pressedDots = currentCellDots,
            onDoubleTap = onDoubleTap,
            isServiceSelectMode = (mode == BrailleMode.SERVICE_SELECT),
            isConfirmSelectMode = (mode == BrailleMode.TRANSFER_CONFIRM),
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, name = "점자 바텀시트 — 금액 입력 중")
@Composable
private fun BrailleBottomSheetContentPreview() {
    SensableTheme {
        BrailleBottomSheetContent(
            guideMessage = "얼마를 보낼까요?",
            currentCellDots = setOf(1, 5),
            inputText = "5000",
            pendingDisplay = "",
            recipientName = "홍길동",
            mode = BrailleMode.TRANSFER_AMOUNT,
            onButtonClick = { _ -> },
            onSwipeRight = {},
            onDoubleTap = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}
