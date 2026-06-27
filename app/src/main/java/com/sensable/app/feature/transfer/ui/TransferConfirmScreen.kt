package com.sensable.app.feature.transfer.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.core.common.postTransferBalance
import com.sensable.app.core.navigation.Screen
import com.sensable.app.feature.transfer.viewmodel.TransferConfirmViewModel
import com.sensable.app.ui.theme.KakaoYellow
import com.sensable.app.ui.theme.SensableTheme

@Composable
fun TransferConfirmScreen(
    navController: NavController,
    recipient: String = "이지영",
    amount: String = "50000",
    viewModel: TransferConfirmViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val formattedAmount = formatAmount(amount)

    val biometricPrompt = remember(context, recipient, amount) {
        BiometricPrompt(
            context as FragmentActivity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    navController.navigate(Screen.TransferComplete.createRoute(recipient, amount))
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 지문 센서 없음·미등록 등 하드웨어 문제 → 목업이므로 그냥 통과
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        navController.navigate(Screen.TransferComplete.createRoute(recipient, amount))
                    }
                }
                override fun onAuthenticationFailed() {
                    // 지문 불일치 — 시스템이 재시도 안내하므로 별도 처리 불필요
                }
            }
        )
    }

    val promptInfo = remember(recipient, formattedAmount) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("지문 인증")
            .setSubtitle("${recipient}님께 $formattedAmount 송금")
            .setNegativeButtonText("취소")
            .build()
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Button(
                onClick = {
                    val canAuthenticate = BiometricManager.from(context).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        // 지문 사용 불가 기기 → 목업이므로 그냥 다음 화면으로
                        navController.navigate(Screen.TransferComplete.createRoute(recipient, amount))
                    }
                },
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
                    InfoRow(label = "이체 후 잔액", value = "%,d원".format(postTransferBalance(amount)), showDivider = false)
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
