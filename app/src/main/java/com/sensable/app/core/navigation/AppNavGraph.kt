package com.sensable.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sensable.app.feature.kakaobank.ui.KakaoBankHomeScreen
import com.sensable.app.feature.transfer.ui.TransferScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.KakaoBankHome.route
    ) {
        composable(Screen.KakaoBankHome.route) {
            KakaoBankHomeScreen(navController = navController)
        }
        composable(Screen.Transfer.route) {
            TransferScreen(navController = navController)
        }
    }
}