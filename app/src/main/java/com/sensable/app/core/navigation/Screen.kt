package com.sensable.app.core.navigation

sealed class Screen(val route: String) {
    data object KakaoBankHome : Screen("kakaobank_home")
    data object TransferConfirm : Screen("transfer_confirm")
    data object TransferComplete : Screen("transfer_complete")
}