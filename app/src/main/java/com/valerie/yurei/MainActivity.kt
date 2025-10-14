package com.valerie.yurei

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.valerie.yurei.ui.navigation.RootNavigator
import com.valerie.yurei.ui.navigation.Route
import com.valerie.yurei.ui.screens.GameScreen
import com.valerie.yurei.ui.screens.HomeScreen
import com.valerie.yurei.ui.screens.SettingsScreen
import com.valerie.yurei.ui.viewmodel.GameViewModel
import android.content.pm.ActivityInfo


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isChromeOS =
            packageManager.hasSystemFeature("android.hardware.type.pc") // Chromebook/Chrome OS

        requestedOrientation = if (isChromeOS) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContent { YureiApp() }
    }
}

@Composable
fun YureiApp() {
    // Thème clair/sombre
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme() else lightColorScheme()

    // Contrôleur de navigation
    val navController = rememberNavController()
    val rootNav = RootNavigator(navController)

    // Instanciation du ViewModel avec le RootNav
    val viewModel = GameViewModel(rootNav)

    MaterialTheme(colorScheme = colors) {
        NavHost(
            navController = navController,
            startDestination = Route.Home.path
        ) {
            // Écran d'accueil
            composable(Route.Home.path) {
                HomeScreen(onStartGame = { viewModel.start() })
            }

            // Écran de jeu
            composable(Route.Game.path) {
                GameScreen(
                    state = viewModel.state,
                    onEvent = { viewModel.event(it) }
                )
            }

            // Écran des paramètres (facultatif)
            composable(Route.Settings.path) {
                SettingsScreen(onBack = { rootNav.back() })
            }
        }
    }
}
