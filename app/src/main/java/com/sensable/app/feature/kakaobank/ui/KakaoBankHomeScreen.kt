package com.sensable.app.feature.kakaobank.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.R
import com.sensable.app.feature.braille.ui.BrailleBottomSheet
import com.sensable.app.feature.kakaobank.viewmodel.KakaoBankViewModel
import com.sensable.app.ui.theme.SensableTheme

@Composable
fun KakaoBankHomeScreen(
    navController: NavController,
    viewModel: KakaoBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    KakaoBankHomeContent(
        isBrailleVisible = uiState.isBrailleVisible,
        onSwipeUp = { viewModel.showBrailleInterface() },
        onDismiss = { viewModel.hideBrailleInterface() },
        navController = navController
    )
}

@Composable
fun KakaoBankHomeContent(
    isBrailleVisible: Boolean,
    onSwipeUp: () -> Unit,
    onDismiss: () -> Unit,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEB00))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -50f) onSwipeUp()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            Image(
                painter = painterResource(id = R.drawable.logo_kakaobank),
                contentDescription = "카카오뱅크 로고",
                modifier = Modifier.height(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "위로 스와이프 → 점자 인터페이스",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (isBrailleVisible) {
            BrailleBottomSheet(
                onDismiss = onDismiss,
                navController = navController
            )
        }
    }
}

@Preview(showSystemUi = true, name = "홈 화면 - 기본")
@Composable
private fun KakaoBankHomeScreenPreview() {
    SensableTheme {
        KakaoBankHomeContent(
            isBrailleVisible = false,
            onSwipeUp = {},
            onDismiss = {},
            navController = rememberNavController()
        )
    }
}
