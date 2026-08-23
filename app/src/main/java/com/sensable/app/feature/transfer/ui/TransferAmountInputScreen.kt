package com.sensable.app.feature.transfer.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.core.common.MOCK_BALANCE
import com.sensable.app.core.navigation.Screen
import com.sensable.app.feature.braille.ui.BrailleBottomSheet
import com.sensable.app.ui.theme.KakaoYellow
import com.sensable.app.ui.theme.SensableTheme
import kotlinx.coroutines.delay

private const val MAX_AMOUNT_DIGITS = 10

@Composable
fun TransferAmountInputScreen(
    navController: NavController,
    recipient: String = "이지영",
    accountNumber: String = "10203040506",
    bankName: String = "카카오뱅크",
) {
    var amount by remember { mutableStateOf("") }
    var showBrailleBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        showBrailleBottomSheet = true
    }

    fun addAmount(delta: Long) {
        val current = amount.toLongOrNull() ?: 0L
        val next = (current + delta).coerceAtMost(MOCK_BALANCE)
        amount = if (next == 0L) "" else next.toString()
    }

    fun appendDigit(digit: String) {
        if (amount.length >= MAX_AMOUNT_DIGITS) return
        val next = (amount + digit).trimStart('0').ifEmpty { "" }
        if ((next.toLongOrNull() ?: 0L) > MOCK_BALANCE) return
        amount = next
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                AmountKeypad(
                    onDigit = { appendDigit(it) },
                    onDoubleZero = { appendDigit("00") },
                    onBackspace = { amount = amount.dropLast(1) }
                )
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    val isEnabled = (amount.toLongOrNull() ?: 0L) > 0L
                    Button(
                        onClick = {
                            navController.navigate(
                                Screen.TransferComplete.createRoute(recipient, amount, accountNumber)
                            )
                        },
                        enabled = isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KakaoYellow,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFF0F0F0),
                            disabledContentColor = Color(0xFFBDBDBD)
                        )
                    ) {
                        Text(
                            text = "다음",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
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
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (amount.isEmpty()) "보낼금액" else "%,d원".format(amount.toLong()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 32.sp,
                    color = if (amount.isEmpty()) Color(0xFFCCCCCC) else Color.Black
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(8.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAmountButton(label = "+1만", modifier = Modifier.weight(1f)) { addAmount(10_000) }
                QuickAmountButton(label = "+5만", modifier = Modifier.weight(1f)) { addAmount(50_000) }
                QuickAmountButton(label = "+10만", modifier = Modifier.weight(1f)) { addAmount(100_000) }
                QuickAmountButton(label = "전액", modifier = Modifier.weight(1f)) { amount = MOCK_BALANCE.toString() }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showBrailleBottomSheet) {
        BrailleBottomSheet(
            onDismiss = { showBrailleBottomSheet = false },
            navController = navController,
            startInAmountMode = true,
            initialRecipientName = recipient,
            initialAccountNumber = accountNumber,
        )
    }
}

@Composable
private fun QuickAmountButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp)
    }
}

@Composable
private fun AmountKeypad(
    onDigit: (String) -> Unit,
    onDoubleZero: () -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    KeypadCell(modifier = Modifier.weight(1f), onClick = { onDigit(digit) }) {
                        Text(text = digit, fontSize = 26.sp, color = Color.Black)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            KeypadCell(modifier = Modifier.weight(1f), onClick = onDoubleZero) {
                Text(text = "00", fontSize = 26.sp, color = Color.Black)
            }
            KeypadCell(modifier = Modifier.weight(1f), onClick = { onDigit("0") }) {
                Text(text = "0", fontSize = 26.sp, color = Color.Black)
            }
            KeypadCell(modifier = Modifier.weight(1f), onClick = onBackspace) {
                Icon(imageVector = Icons.Default.Backspace, contentDescription = "지우기", tint = Color.Black)
            }
        }
    }
}

@Composable
private fun KeypadCell(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(showSystemUi = true, name = "이체 - 송금액 입력")
@Composable
private fun TransferAmountInputScreenPreview() {
    SensableTheme {
        TransferAmountInputScreen(navController = rememberNavController())
    }
}
