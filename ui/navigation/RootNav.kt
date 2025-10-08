// ui/RootNav.kt
package com.valerie.yurei.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.valerie.yurei.ui.screens.GameScreen

@Composable
fun RootNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "game") {
        composable("game") { GameScreen() }
    }
}
