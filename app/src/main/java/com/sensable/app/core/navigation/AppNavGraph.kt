package com.sensable.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sensable.app.feature.kakaobank.ui.KakaoBankHomeScreen
import com.sensable.app.feature.transfer.ui.TransferAccountInputScreen
import com.sensable.app.feature.transfer.ui.TransferAmountInputScreen
import com.sensable.app.feature.transfer.ui.TransferCompleteScreen
import com.sensable.app.feature.transfer.ui.TransferMemoScreen
import com.sensable.app.feature.transfer.ui.TransferRecipientScreen
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
        composable(Screen.TransferRecipient.route) {
            TransferRecipientScreen(navController = navController)
        }
        composable(Screen.TransferAccountInput.route) {
            TransferAccountInputScreen(navController = navController)
        }
        composable(
            route = Screen.TransferAmountInput.route,
            arguments = listOf(
                navArgument("recipient") { type = NavType.StringType },
                navArgument("accountNumber") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val enc = "UTF-8"
            val recipient = URLDecoder.decode(backStackEntry.arguments?.getString("recipient") ?: "", enc)
            val accountNumber = URLDecoder.decode(backStackEntry.arguments?.getString("accountNumber") ?: "", enc)
            TransferAmountInputScreen(
                navController = navController,
                recipient = recipient,
                accountNumber = accountNumber,
            )
        }
        composable(
            route = Screen.TransferMemo.route,
            arguments = listOf(
                navArgument("recipient") { type = NavType.StringType },
                navArgument("accountNumber") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val enc = "UTF-8"
            val recipient = URLDecoder.decode(backStackEntry.arguments?.getString("recipient") ?: "", enc)
            val accountNumber = URLDecoder.decode(backStackEntry.arguments?.getString("accountNumber") ?: "", enc)
            val amount = URLDecoder.decode(backStackEntry.arguments?.getString("amount") ?: "", enc)
            TransferMemoScreen(
                navController = navController,
                recipient = recipient,
                accountNumber = accountNumber,
                amount = amount,
            )
        }
        composable(
            route = Screen.TransferComplete.route,
            arguments = listOf(
                navArgument("recipient") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
                navArgument("accountNumber") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val enc = "UTF-8"
            val recipient = URLDecoder.decode(backStackEntry.arguments?.getString("recipient") ?: "", enc)
            val amount = URLDecoder.decode(backStackEntry.arguments?.getString("amount") ?: "", enc)
            val accountNumber = URLDecoder.decode(backStackEntry.arguments?.getString("accountNumber") ?: "", enc)
            TransferCompleteScreen(
                navController = navController,
                recipient = recipient,
                amount = amount,
                accountNumber = accountNumber,
            )
        }
    }
}
