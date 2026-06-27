package com.sensable.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.sensable.app.core.navigation.AppNavGraph
import com.sensable.app.core.tts.TtsManager
import com.sensable.app.ui.theme.SensableTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // @Inject 어노테이션을 통해 Hilt로 의존성 주입을 관리하고 있다는 것을 알수 있음
    // lateinit이 붙는 이유는? (나중에 초기화)
    @Inject lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensableTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }

    // TTS가 시스템 리소스를 잡아먹기 때문에, 생명주기를 고려하여 추가한 코드
    // 만약 해제 안하면 어떤 큰일이 나길래?
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) ttsManager.shutdown()
    }
}
