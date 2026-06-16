package com.sensable.app.core.navigation

sealed class Screen(val route: String) {
    data object KakaoBankHome : Screen("kakaobank_home")
    data object Transfer : Screen("transfer")
}