package com.sensable.app.feature.transfer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sensable.app.core.designsystem.component.BrailleGrid
import com.sensable.app.feature.transfer.viewmodel.TransferStep
import com.sensable.app.feature.transfer.viewmodel.TransferUiState
import com.sensable.app.feature.transfer.viewmodel.TransferViewModel
import com.sensable.app.ui.theme.SensableTheme

@Composable
fun TransferScreen(
    navController: NavController,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    TransferContent(
        uiState = uiState,
        onBrailleInput = viewModel::onBrailleInput,
        onConfirm = viewModel::onConfirm
    )
}

@Composable
internal fun TransferContent(
    uiState: TransferUiState,
    onBrailleInput: (row: Int, col: Int) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = uiState.guideMessage,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = uiState.inputDisplay.ifEmpty { "입력 중..." },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        BrailleGrid(
            onButtonClick = onBrailleInput,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text(text = if (uiState.isLoading) "처리 중..." else "확인")
        }
    }
}

@Preview(showBackground = true, name = "송금 - 받는 사람 입력")
@Composable
private fun TransferRecipientPreview() {
    SensableTheme {
        TransferContent(
            uiState = TransferUiState(
                step = TransferStep.RECIPIENT,
                guideMessage = "누구에게 보낼까요?",
                inputDisplay = ""
            ),
            onBrailleInput = { _, _ -> },
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true, name = "송금 - 금액 입력")
@Composable
private fun TransferAmountPreview() {
    SensableTheme {
        TransferContent(
            uiState = TransferUiState(
                step = TransferStep.AMOUNT,
                guideMessage = "김철수님께 얼마를 보낼까요?",
                inputDisplay = "10,000"
            ),
            onBrailleInput = { _, _ -> },
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true, name = "송금 - 최종 확인")
@Composable
private fun TransferConfirmPreview() {
    SensableTheme {
        TransferContent(
            uiState = TransferUiState(
                step = TransferStep.CONFIRM,
                guideMessage = "김철수님께 10,000원 송금하는게 맞습니까?",
                inputDisplay = "10,000"
            ),
            onBrailleInput = { _, _ -> },
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true, name = "송금 - 처리 중")
@Composable
private fun TransferLoadingPreview() {
    SensableTheme {
        TransferContent(
            uiState = TransferUiState(
                step = TransferStep.CONFIRM,
                guideMessage = "김철수님께 10,000원 송금하는게 맞습니까?",
                inputDisplay = "10,000",
                isLoading = true
            ),
            onBrailleInput = { _, _ -> },
            onConfirm = {}
        )
    }
}
