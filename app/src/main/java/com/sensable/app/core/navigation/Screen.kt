package com.sensable.app.core.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object KakaoBankHome : Screen("kakaobank_home")
    data object TransferConfirm : Screen("transfer_confirm/{recipient}/{amount}") {
        fun createRoute(recipient: String, amount: String): String {
            val enc = "UTF-8"
            return "transfer_confirm/${URLEncoder.encode(recipient, enc)}/${URLEncoder.encode(amount, enc)}"
        }
    }
    data object TransferComplete : Screen("transfer_complete/{recipient}/{amount}") {
        fun createRoute(recipient: String, amount: String): String {
            val enc = "UTF-8"
            return "transfer_complete/${URLEncoder.encode(recipient, enc)}/${URLEncoder.encode(amount, enc)}"
        }
    }
}
