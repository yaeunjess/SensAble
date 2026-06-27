package com.sensable.app.feature.kakaobank.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sensable.app.R
import com.sensable.app.core.common.MOCK_BALANCE
import com.sensable.app.feature.braille.ui.BrailleBottomSheet
import com.sensable.app.feature.kakaobank.viewmodel.KakaoBankViewModel
import com.sensable.app.ui.theme.SensableTheme

private val KakaoBackground = Color(0xFFF7F7F7)
private val KakaoBlue = Color(0xFFA2B5E8)

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
    val scrollState = rememberScrollState()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y < -500f) {
                    onSwipeUp()
                }
                return Velocity.Zero
            }
        }
    }

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
                .nestedScroll(nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BannerSection()
                AccountCard(userName = "김예은", balance = "%,d원".format(MOCK_BALANCE))
                ServicePromotionCard()
                AddServiceCard()
                FooterLinks()
                QuickAccessGrid()
                
                Spacer(modifier = Modifier.height(32.dp))
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

@Composable
private fun BannerSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("배너 영역 (이미지 예정)", color = Color.LightGray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AccountCard(userName: String, balance: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KakaoBlue,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_kakaobank),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${userName}의 통장 ★",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = balance,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 44.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountSmallButton(text = "카드")
                AccountSmallButton(text = "이체")
            }
        }
    }
}

@Composable
private fun AccountSmallButton(text: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        onClick = {}
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ServicePromotionCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFE0B2), RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "은행, 병원, 편의점 어디서나",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "이제 신분증도 앱으로",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AddServiceCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        onClick = {}
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun FooterLinks() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("간편 홈", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .height(12.dp)
                .width(1.dp)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text("화면 편집", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun QuickAccessGrid() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAccessCard("신용대출\n비교하기", weight = 1f)
        QuickAccessCard("응모하고\n혜택받기", weight = 1f)
        QuickAccessCard("생활비\n돌려받기", weight = 1f)
    }
}

@Composable
private fun RowScope.QuickAccessCard(title: String, weight: Float) {
    Surface(
        modifier = Modifier
            .weight(weight)
            .aspectRatio(0.85f),
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFE3F2FD), CircleShape)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KakaoBankTopBar(userName: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEEEEEE),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "내 계좌",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
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
            containerColor = KakaoBackground
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
