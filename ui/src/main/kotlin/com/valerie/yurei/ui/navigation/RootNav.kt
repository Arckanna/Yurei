package com.valerie.yurei.ui.navigation

import androidx.navigation.NavHostController

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Game : Route("game")
    data object Settings : Route("settings")
}

interface RootNav {
    fun toHome()
    fun toGame()
    fun toSettings()
    fun back()
}

class RootNavigator(private val nav: NavHostController) : RootNav {
    override fun toHome() = nav.navigate(Route.Home.path) {
        popUpTo(0) { inclusive = true } // reset stack
        launchSingleTop = true
    }
    override fun toGame() = nav.navigate(Route.Game.path) {
        popUpTo(Route.Home.path) { inclusive = false }
        launchSingleTop = true
    }
    override fun toSettings() = nav.navigate(Route.Settings.path) {
        launchSingleTop = true
    }
    override fun back() {
        nav.popBackStack()
    }
}
