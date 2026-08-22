package com.sensable.app.core.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object KakaoBankHome : Screen("kakaobank_home")
    data object TransferRecipient : Screen("transfer_recipient")
    data object TransferAccountInput : Screen("transfer_account_input")
    data object TransferAmountInput : Screen("transfer_amount_input/{recipient}/{accountNumber}") {
        fun createRoute(recipient: String, accountNumber: String): String {
            val enc = "UTF-8"
            return "transfer_amount_input/${URLEncoder.encode(recipient, enc)}/${URLEncoder.encode(accountNumber, enc)}"
        }
    }
    data object TransferMemo : Screen("transfer_memo/{recipient}/{accountNumber}/{amount}") {
        fun createRoute(recipient: String, accountNumber: String, amount: String): String {
            val enc = "UTF-8"
            return "transfer_memo/${URLEncoder.encode(recipient, enc)}/${URLEncoder.encode(accountNumber, enc)}/${URLEncoder.encode(amount, enc)}"
        }
    }
    data object TransferComplete : Screen("transfer_complete/{recipient}/{amount}/{accountNumber}") {
        fun createRoute(recipient: String, amount: String, accountNumber: String = "10203040506"): String {
            val enc = "UTF-8"
            return "transfer_complete/${URLEncoder.encode(recipient, enc)}/${URLEncoder.encode(amount, enc)}/${URLEncoder.encode(accountNumber, enc)}"
        }
    }
}
