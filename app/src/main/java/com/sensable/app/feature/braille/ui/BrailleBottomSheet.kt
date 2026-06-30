package com.sensable.app.feature.braille.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sensable.app.ui.theme.SensableBlueContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sensable.app.core.designsystem.component.BrailleGrid
import com.sensable.app.core.navigation.Screen
import com.sensable.app.feature.braille.viewmodel.BrailleMode
import com.sensable.app.feature.braille.viewmodel.BrailleViewModel
import com.sensable.app.ui.theme.SensableTheme

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
        )
    ) {
        BrailleBottomSheetContent(
            guideMessage = uiState.guideMessage,
            currentCellDots = uiState.currentCellDots,
            inputText = uiState.inputText,
            pendingDisplay = uiState.pendingDisplay,
            recipientName = uiState.recipientName,
            autocompleteSuggestion = uiState.autocompleteSuggestion,
            mode = uiState.mode,
            onButtonClick = { dot -> viewModel.onBrailleButtonClick(dot) },
            onSwipeRight = { viewModel.onSwipeRight() },
            onSwipeLeft = { if (viewModel.onSwipeLeft()) onDismiss() },
            onSwipeUp = { viewModel.onSwipeUp() },
            onDoubleTap = {
                viewModel.onDoubleTap { recipient, amount ->
                    navController.navigate(Screen.TransferConfirm.createRoute(recipient, amount))
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(vertical = 16.dp)
        )
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
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 더블탭 감지 영역 (가이드 메시지 + 디코딩 텍스트)
        // 확인 단계는 2줄 문구이므로 높이를 조금 더 확보하고, 나머지 단계는 고정 높이로 버튼 위치 유지
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (mode == BrailleMode.TRANSFER_CONFIRM)
                        Modifier.heightIn(min = 160.dp)
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
            // 금액 입력 단계: 수취인 이름 + 안내 문구를 한 줄로 합쳐서 높이 동일하게 유지
            val guideAnnotated = if (mode == BrailleMode.TRANSFER_AMOUNT && recipientName.isNotEmpty()) {
                buildAnnotatedString {
                    withStyle(SpanStyle(color = SensableBlueContent, fontWeight = FontWeight.SemiBold)) {
                        append(recipientName)
                    }
                    append("님에게 얼마를 보낼까요?")
                }
            } else {
                buildAnnotatedString { append(guideMessage) }
            }

            Text(
                text = guideAnnotated,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            // 텍스트 표시 영역 — 우선순위: 자동완성/교정 후보 > 원본 입력 > recipientName(오타교정 모드)
            val displayAnnotated = when {
                autocompleteSuggestion.isNotEmpty() -> buildAnnotatedString {
                    withStyle(SpanStyle(color = SensableBlueContent, fontWeight = FontWeight.Bold)) {
                        append(autocompleteSuggestion)
                    }
                }
                inputText.isNotEmpty() || pendingDisplay.isNotEmpty() -> buildAnnotatedString {
                    val displayInput = if (mode == BrailleMode.TRANSFER_AMOUNT && inputText.isNotEmpty()) {
                        "%,d원".format(inputText.toLongOrNull() ?: 0L)
                    } else inputText
                    withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                        append(displayInput)
                    }
                    if (pendingDisplay.isNotEmpty()) {
                        withStyle(SpanStyle(color = Color.Gray)) {
                            if (inputText.isNotEmpty()) append(" | ")
                            append(pendingDisplay)
                        }
                    }
                }
                mode == BrailleMode.TYPO_CORRECTION && recipientName.isNotEmpty() -> buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
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
