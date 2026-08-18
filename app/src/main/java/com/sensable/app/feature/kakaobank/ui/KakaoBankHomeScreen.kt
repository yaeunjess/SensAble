package com.sensable.app.feature.kakaobank.ui

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sensable.app.R
import com.sensable.app.core.common.MOCK_BALANCE
import com.sensable.app.core.navigation.Screen
import com.sensable.app.feature.braille.ui.BrailleBottomSheet
import com.sensable.app.ui.theme.SensableTheme
import com.finclue.sdk.Finclue
import com.finclue.sdk.api.FieldSpec
import com.finclue.sdk.api.FieldType
import com.finclue.sdk.api.FlowResult
import com.finclue.sdk.api.FlowSpec
import com.finclue.sdk.storage.LocalDataSummary
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

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
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLocalData by remember { mutableStateOf(false) }
    var showBrailleTransfer by remember { mutableStateOf(false) }
    var localData by remember { mutableStateOf<LocalDataSummary?>(null) }

    fun refreshLocalData() {
        scope.launch { localData = Finclue.getLocalDataSummary(context) }
    }

    KakaoBankHomeContent(
        onSwipeUp = {
            showBrailleTransfer = true
        },
        onShowLocalData = {
            showLocalData = true
            refreshLocalData()
        },
    )

    if (showBrailleTransfer) {
        BrailleBottomSheet(
            onDismiss = { showBrailleTransfer = false },
            navController = navController,
        )
    }

    if (showLocalData) {
        LocalDataDialog(
            summary = localData,
            onDismiss = { showLocalData = false },
            onClear = {
                scope.launch {
                    Finclue.clearLocalData(context)
                    localData = Finclue.getLocalDataSummary(context)
                }
            },
        )
    }
}

private fun transferFlowSpec() = FlowSpec(
    fields = listOf(
        FieldSpec(
            key = "toAccount",
            type = FieldType.ACCOUNT,
            label = "받는 계좌",
            prompt = "받는 분 계좌번호를 입력하세요",
        ),
        FieldSpec(
            key = "amount",
            type = FieldType.AMOUNT,
            label = "보낼 금액",
            prompt = "보낼 금액을 입력하세요",
        ),
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KakaoBankHomeContent(
    onSwipeUp: () -> Unit,
    onShowLocalData: () -> Unit,
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val swipeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val nestedScrollConnection = remember(onSwipeUp, swipeThreshold) {
        object : NestedScrollConnection {
            var upwardDistance = 0f
            var triggered = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && !triggered) {
                    upwardDistance += -available.y
                }
                if (upwardDistance >= swipeThreshold && !triggered) {
                    triggered = true
                    onSwipeUp()
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                upwardDistance = 0f
                triggered = false
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
                AccountCard(
                    userName = "김예은",
                    balance = "%,d원".format(MOCK_BALANCE),
                    onTransferClick = onSwipeUp,
                )
                OutlinedButton(
                    onClick = onShowLocalData,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("FIN:CLUE 온디바이스 데이터 확인")
                }
                ServicePromotionCard()
                AddServiceCard()
                FooterLinks()
                QuickAccessGrid()
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LocalDataDialog(
    summary: LocalDataSummary?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FIN:CLUE 온디바이스 데이터") },
        text = {
            if (summary == null) {
                CircularProgressIndicator()
            } else {
                Text(
                    "네트워크 권한: 없음\n" +
                        "저장 위치: 앱 전용 Room DB\n\n" +
                        "전체 실행: ${summary.totalSessions}회\n" +
                        "완료: ${summary.completedSessions}회\n" +
                        "취소: ${summary.cancelledSessions}회\n" +
                        "오류: ${summary.errorSessions}회\n" +
                        "기록된 필드: ${summary.recordedFields}개\n" +
                        "평균 완료 시간: ${summary.averageCompletedDurationMillis ?: 0L}ms"
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("확인") } },
        dismissButton = { TextButton(onClick = onClear) { Text("로컬 데이터 초기화") } },
    )
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
private fun AccountCard(userName: String, balance: String, onTransferClick: () -> Unit) {
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
                AccountSmallButton(text = "카드", onClick = {})
                AccountSmallButton(text = "이체", onClick = onTransferClick)
            }
        }
    }
}

@Composable
private fun AccountSmallButton(text: String, onClick: () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
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
            onSwipeUp = {},
            onShowLocalData = {},
        )
    }
}
