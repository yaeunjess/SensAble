package com.sensable.app.feature.transfer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.core.common.MOCK_BALANCE
import com.sensable.app.core.designsystem.component.FingerprintAuthOverlay
import com.sensable.app.core.navigation.Screen
import com.sensable.app.ui.theme.KakaoYellow
import com.sensable.app.ui.theme.SensableTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMemoScreen(
    navController: NavController,
    recipient: String = "현준혁",
    accountNumber: String = "10203040506",
    amount: String = "0",
    bankName: String = "카카오뱅크",
) {
    var showConfirmSheet by remember { mutableStateOf(false) }
    var showFingerprintOverlay by remember { mutableStateOf(false) }
    val formattedAmount = "%,d원".format(amount.toLongOrNull() ?: 0L)

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { showConfirmSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KakaoYellow,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "다음",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.Black
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recipient,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                    Text(
                        text = "$bankName $accountNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9E9E)
                    )
                }
                TextButton(onClick = {
                    navController.navigate(Screen.KakaoBankHome.route) {
                        popUpTo(Screen.KakaoBankHome.route) { inclusive = true }
                    }
                }) {
                    Text(
                        text = "취소",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%,d원".format(amount.toLongOrNull() ?: 0L),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 32.sp,
                    color = Color.Black
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "김예은의 통장 (1234) : %,d원".format(MOCK_BALANCE),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E)
                        )
                    }
                }

                MemoRow(
                    label = "받는 분에게 표기",
                    value = recipient,
                    showHelpIcon = true
                )

                MemoRow(
                    label = "나에게 표기",
                    value = "미입력시 수취인명",
                    valueColor = Color(0xFFBDBDBD)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "+ 추가이체",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9E9E)
                    )
                    Text(
                        text = "더보기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9E9E),
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfirmSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
        ) {
            TransferConfirmSheetContent(
                recipient = recipient,
                formattedAmount = formattedAmount,
                bankName = bankName,
                accountNumber = accountNumber,
                onCancel = { showConfirmSheet = false },
                onConfirm = {
                    showConfirmSheet = false
                    showFingerprintOverlay = true
                }
            )
        }
    }

    if (showFingerprintOverlay) {
        FingerprintAuthOverlay(
            recipient = recipient,
            formattedAmount = formattedAmount,
            onDismiss = { showFingerprintOverlay = false },
            onAuthSuccess = {
                showFingerprintOverlay = false
                navController.navigate(
                    Screen.TransferComplete.createRoute(recipient, amount, accountNumber)
                )
            }
        )
    }
    }
}

@Composable
private fun TransferConfirmSheetContent(
    recipient: String,
    formattedAmount: String,
    bankName: String,
    accountNumber: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF8A7863),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "K",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = KakaoYellow
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "${recipient}님에게 $formattedAmount\n이체하시겠습니까?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "받는계좌 : $bankName $accountNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9E9E9E)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "취소",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KakaoYellow,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "이체하기",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun MemoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black,
    showHelpIcon: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
        if (showHelpIcon) {
            Spacer(modifier = Modifier.padding(start = 4.dp))
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.height(16.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor
        )
    }
}

@Preview(showSystemUi = true, name = "이체 - 메모 입력")
@Composable
private fun TransferMemoScreenPreview() {
    SensableTheme {
        TransferMemoScreen(navController = rememberNavController(), amount = "1")
    }
}
