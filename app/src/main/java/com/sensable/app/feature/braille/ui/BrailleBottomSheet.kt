package com.sensable.app.feature.braille.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sensable.app.core.designsystem.component.BrailleGrid
import com.sensable.app.core.navigation.Screen
import com.sensable.app.feature.braille.viewmodel.BrailleMode
import com.sensable.app.feature.braille.viewmodel.BrailleViewModel
import com.sensable.app.ui.theme.SensableBlue
import com.sensable.app.ui.theme.SensableDarkOnSurface
import com.sensable.app.ui.theme.SensableDarkSurface
import com.sensable.app.ui.theme.SensableTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrailleBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    startInAccountNumberMode: Boolean = false,
    startInAmountMode: Boolean = false,
    initialRecipientName: String = "",
    initialAccountNumber: String = "",
    onBackPress: (() -> Unit)? = null,
    viewModel: BrailleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        when {
            startInAccountNumberMode -> viewModel.startAccountNumberEntry()
            startInAmountMode -> viewModel.startAmountEntry(initialRecipientName)
        }
    }

    LaunchedEffect(uiState.accountEntryCompleted) {
        val result = uiState.accountEntryCompleted ?: return@LaunchedEffect
        onDismiss()
        navController.navigate(
            Screen.TransferAmountInput.createRoute(result.recipientName, result.accountNumber)
        )
    }

    LaunchedEffect(uiState.amountEntryCompleted) {
        val amount = uiState.amountEntryCompleted ?: return@LaunchedEffect
        onDismiss()
        navController.navigate(
            Screen.TransferMemo.createRoute(initialRecipientName, initialAccountNumber, amount)
        )
    }

    ModalBottomSheet(
        modifier = Modifier.semantics {
            paneTitle = ""
        },
        onDismissRequest = { onBackPress?.invoke() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false }
        ),
        containerColor = SensableDarkSurface.copy(alpha = 0.5f),
        contentColor = SensableDarkOnSurface.copy(alpha = 0.7f),
        dragHandle = null,
    ) {
        // ModalBottomSheet는 별도 Android Dialog/Window로 떠서, 제목을 지정하지 않으면
        // TalkBack이 창 전환 시 앱 라벨("카카오뱅크")을 대신 읽는다. 빈 접근성 제목을 명시해 막는다.
        val dialogWindowView = LocalView.current
        LaunchedEffect(dialogWindowView) {
            var parent = dialogWindowView.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            val window = (parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
            window.setTitle(" ")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            BrailleBottomSheetContent(
                guideMessage = uiState.guideMessage,
                currentCellDots = uiState.currentCellDots,
                inputText = when (uiState.mode) {
                    BrailleMode.TRANSFER_RECIPIENT -> uiState.inputText + uiState.pendingDisplay
                    BrailleMode.AI_RECOMMENDATION -> uiState.autocompleteSuggestion
                    else -> uiState.inputText
                },
                recipientName = uiState.recipientName,
                mode = uiState.mode,
                onButtonClick = { dot -> viewModel.onBrailleButtonClick(dot) },
                onSwipeRight = {
                    if (uiState.mode == BrailleMode.TRANSFER_RECIPIENT &&
                        uiState.currentCellDots == setOf(1, 2, 3, 4, 5, 6)
                    ) {
                        viewModel.onAiCorrection()
                    } else {
                        viewModel.onSwipeRight()
                    }
                },
                onSwipeLeft = { if (viewModel.onSwipeLeft()) onDismiss() },
                onAiCorrection = { viewModel.onAiCorrection() },
                onDoubleTap = { viewModel.onDoubleTap() },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
internal fun BrailleBottomSheetContent(
    guideMessage: String,
    currentCellDots: Set<Int>,
    inputText: String,
    recipientName: String,
    mode: BrailleMode,
    onButtonClick: (dot: Int) -> Unit,
    onSwipeRight: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeLeft: (() -> Unit)? = null,
    onAiCorrection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() })
                }
                // 안내문구/입력값은 시각 확인용이며 동일한 내용을 TtsManager가 이미
                // 음성으로 안내하므로 TalkBack이 중복 낭독하지 않도록 숨긴다.
                .clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val guideAnnotated = when {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            if (inputText.isNotEmpty()) {
                val displayInput = if (mode == BrailleMode.TRANSFER_AMOUNT) {
                    "%,d원".format(inputText.toLongOrNull() ?: 0L)
                } else inputText
                Text(
                    text = displayInput,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SensableDarkOnSurface,
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
            pressedDots = currentCellDots,
            onDoubleTap = onDoubleTap,
            onAiCorrection = if (mode == BrailleMode.TRANSFER_RECIPIENT ||
                mode == BrailleMode.AI_RECOMMENDATION
            ) {
                onAiCorrection
            } else null,
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
