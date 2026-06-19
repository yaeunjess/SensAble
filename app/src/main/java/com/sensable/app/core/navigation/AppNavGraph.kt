package com.sensable.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sensable.app.feature.kakaobank.ui.KakaoBankHomeScreen
import com.sensable.app.feature.transfer.ui.TransferCompleteScreen
import com.sensable.app.feature.transfer.ui.TransferConfirmScreen
import java.net.URLDecoder

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.KakaoBankHome.route
    ) {
        composable(Screen.KakaoBankHome.route) {
            KakaoBankHomeScreen(navController = navController)
        }
        composable(
            route = Screen.TransferConfirm.route,
            arguments = listOf(
                navArgument("recipient") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val enc = "UTF-8"
            val recipient = URLDecoder.decode(backStackEntry.arguments?.getString("recipient") ?: "", enc)
            val amount = URLDecoder.decode(backStackEntry.arguments?.getString("amount") ?: "", enc)
            TransferConfirmScreen(navController = navController, recipient = recipient, amount = amount)
        }
        composable(
            route = Screen.TransferComplete.route,
            arguments = listOf(
                navArgument("recipient") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val enc = "UTF-8"
            val recipient = URLDecoder.decode(backStackEntry.arguments?.getString("recipient") ?: "", enc)
            val amount = URLDecoder.decode(backStackEntry.arguments?.getString("amount") ?: "", enc)
            TransferCompleteScreen(navController = navController, recipient = recipient, amount = amount)
        }
    }
}
