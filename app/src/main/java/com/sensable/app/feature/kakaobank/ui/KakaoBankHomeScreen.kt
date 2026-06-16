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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.R
import com.sensable.app.feature.braille.ui.BrailleBottomSheet
import com.sensable.app.feature.kakaobank.viewmodel.KakaoBankViewModel
import com.sensable.app.ui.theme.SensableTheme

private val KakaoBackground = Color(0xFFFFFF)

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("홈", Icons.Default.Home),
    BottomNavItem("혜택", Icons.Default.CreditCard),
    BottomNavItem("상품", Icons.Default.AllInbox),
    BottomNavItem("전체", Icons.Default.Menu)
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KakaoBankHomeContent(
    isBrailleVisible: Boolean,
    onSwipeUp: () -> Unit,
    onDismiss: () -> Unit,
    navController: NavController
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            KakaoBankTopBar(userName = "김예은")
        },
        bottomBar = {
            KakaoBankBottomNavigationBar(
                selectedIndex = selectedNavIndex,
                onItemSelected = { selectedNavIndex = it }
            )
        },
        containerColor = KakaoBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(KakaoBackground)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -50f) onSwipeUp()
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KakaoBankTopBar(userName: String) {
    TopAppBar(
        title = {
            Text(
                text = "${userName}님",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "알림",
                    tint = Color.Black
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun KakaoBankBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = Color.Black,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
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