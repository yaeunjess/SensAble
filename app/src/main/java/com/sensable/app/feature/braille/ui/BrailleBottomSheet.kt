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
import androidx.compose.runtime.SideEffect
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
import com.sensable.app.core.designsystem.component.rememberTouchExplorationEnabled
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
        // 바텀시트가 뜰 때 TalkBack이 다이얼로그 창 제목(앱 이름)을 읽어버리는 것을 방지.
        // 이 다이얼로그는 FEATURE_NO_TITLE로 만들어져 있어 Window.setTitle()은 타이틀 뷰가 없어 무시되므로,
        // TalkBack이 실제로 참조하는 WindowManager.LayoutParams.title을 직접 갱신해야 함.
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.let { window ->
                window.attributes = window.attributes.apply { title = " " }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            BrailleBottomSheetContent(
                guideMessage = uiState.guideMessage,
                currentCellDots = uiState.currentCellDots,
                inputText = uiState.inputText,
                recipientName = uiState.recipientName,
                mode = uiState.mode,
                onButtonClick = { dot -> viewModel.onBrailleButtonClick(dot) },
                onSwipeRight = { viewModel.onSwipeRight() },
                onSwipeLeft = { if (viewModel.onSwipeLeft()) onDismiss() },
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
    modifier: Modifier = Modifier
) {
    val touchExplorationEnabled = rememberTouchExplorationEnabled()

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
                .then(
                    // TTS가 이미 안내를 읽어주므로 TalkBack이 화면 텍스트를 중복으로 읽지 않도록 접근성 트리에서 제외
                    if (touchExplorationEnabled) Modifier.clearAndSetSemantics { } else Modifier
                ),
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
