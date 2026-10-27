package com.valerie.yurei

import android.app.Application
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.valerie.yurei.ui.viewmodel.GameIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.valerie.yurei.ui.navigation.RootNavigator
import com.valerie.yurei.ui.navigation.Route
import com.valerie.yurei.ui.screens.GameScreen
import com.valerie.yurei.ui.screens.HomeScreen
import com.valerie.yurei.ui.screens.SettingsScreen
import com.valerie.yurei.ui.viewmodel.GameViewModel
import com.valerie.yurei.ui.viewmodel.GameViewModelFactory
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
    val rootNav = remember(navController) { RootNavigator(navController) }
    val application = LocalContext.current.applicationContext as Application

    // ViewModel avec factory (audio + sauvegarde)
    val viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(application, rootNav)
    )

    SideEffect { viewModel.updateNavigator(rootNav) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }

    val persistenceError by remember(viewModel) {
        viewModel.state.map { it.persistenceError }.distinctUntilChanged()
    }.collectAsState(initial = null)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(persistenceError) {
        persistenceError?.let { snackbar.showSnackbar(it) }
    }
    MaterialTheme(colorScheme = colors) {
        Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Route.Home.path
        ) {
            // Écran d'accueil
            composable(Route.Home.path) {
                val uiState by viewModel.state.collectAsState()
                HomeScreen(
                    onStartGame = { viewModel.event(GameIntent.StartLevel(1)) },
                    onSakura = { viewModel.event(GameIntent.StartLevel(2)) },
                    sakuraMemoryUnlocked = uiState.sakuraMemoryUnlocked,
                    highScore = uiState.highScore,
                    ready = uiState.ready,
                    memoryUnlocked = uiState.memoryUnlocked,
                    hasSavedGame = uiState.hasSavedGame,
                    onContinue = { viewModel.event(GameIntent.Continue) },
                    onOpenSettings = { rootNav.toSettings() }
                )
            }

            // Écran de jeu
            composable(Route.Game.path) {
                GameScreen(
                    state = viewModel.state,
                    onEvent = { viewModel.event(it) },
                    onUpdateWorldSize = { size -> viewModel.updateWorldSize(size) },
                    onOpenSettings = { rootNav.toSettings() }
                )
            }

            // Écran des paramètres
            composable(Route.Settings.path) {
                val uiState by viewModel.state.collectAsState()
                SettingsScreen(
                    onBack = { rootNav.back() },
                    musicEnabled = uiState.musicEnabled,
                    sfxEnabled = uiState.sfxEnabled,
                    highScore = uiState.highScore,
                    onMusicEnabledChange = { viewModel.setMusicEnabled(it) },
                    onSfxEnabledChange = { viewModel.setSfxEnabled(it) }
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}
