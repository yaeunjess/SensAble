package com.sensable.app.feature.transfer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.feature.braille.ui.BrailleBottomSheet
import com.sensable.app.ui.theme.SensableTheme
import kotlinx.coroutines.delay

@Composable
fun TransferAccountInputScreen(
    navController: NavController,
) {
    var query by remember { mutableStateOf("") }
    var showBrailleBottomSheet by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        delay(1000)
        showBrailleBottomSheet = true
    }

    Scaffold(
        containerColor = Color.White
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
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    readOnly = true,
                    placeholder = {
                        Text(
                            text = "받는사람 이름 또는 계좌번호",
                            color = Color(0xFF9E9E9E)
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.Black),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            Spacer(modifier = Modifier.height(120.dp))

            Text(
                text = buildAnnotatedString {
                    append("받는사람의 ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("이름, 닉네임, 은행명, 계좌번호") }
                    append("를 찾거나\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("직접 계좌번호를 입력") }
                    append("하여 이체할 수 있습니다.")
                },
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 14.sp,
                lineHeight = 24.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
        }
    }

    if (showBrailleBottomSheet) {
        BrailleBottomSheet(
            onDismiss = { showBrailleBottomSheet = false },
            navController = navController,
            startInAccountNumberMode = true,
            onBackPress = { navController.popBackStack() },
        )
    }
}

@Preview(showSystemUi = true, name = "이체 - 계좌번호 입력")
@Composable
private fun TransferAccountInputScreenPreview() {
    SensableTheme {
        TransferAccountInputScreen(navController = rememberNavController())
    }
}
