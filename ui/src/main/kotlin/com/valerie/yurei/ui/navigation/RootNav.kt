package com.valerie.yurei.ui.navigation

import androidx.navigation.NavHostController

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Game : Route("game")
    data object Settings : Route("settings")
    /** Prototype du labyrinthe (étape 2) ; remplacera Game quand les règles seront complètes. */
    data object Maze : Route("maze")
}

interface RootNav {
    fun toHome()
    fun toGame()
    fun toSettings()
    fun back()
    /** Implémentation vide par défaut : les navigateurs de test n'ont pas à la fournir. */
    fun toMaze() = Unit
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
    override fun toMaze() = nav.navigate(Route.Maze.path) {
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
