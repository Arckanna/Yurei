package com.valerie.yurei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.valerie.yurei.ui.components.PauseOverlay
import com.valerie.yurei.ui.viewmodel.GameIntent
import com.valerie.yurei.ui.viewmodel.GamePhase
import com.valerie.yurei.ui.viewmodel.GameUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GameScreen(
    state: StateFlow<GameUiState>,
    onEvent: (GameIntent) -> Unit,
    onOpenSettings: (() -> Unit)? = null, // optionnel si tu veux ouvrir Settings depuis l’overlay
) {
    val ui by state.collectAsState()

    // Touche "retour" → ouvrir Pause quand on joue
    BackHandler(enabled = ui.phase == GamePhase.Running) {
        onEvent(GameIntent.Pause)
    }

    Column(Modifier.fillMaxSize()) {
        // Petit HUD
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Score: ${ui.score}")
            Text("FPS: ${ui.fps}")
        }

        // Zone de jeu + drag (désactivé quand Paused ou GameOver)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (ui.phase == GamePhase.Running) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { _, drag ->
                                onEvent(GameIntent.Drag(drag.x, drag.y))
                            }
                        }
                    } else {
                        Modifier // pas d’input quand en pause
                    }
                )
        ) {
            // Dragon minimaliste : un cercle à la position (x, y)
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val d = ui.dragon
                drawCircle(
                    color = Color(0xFF64B5F6),
                    radius = 24f + 24f * d.light,
                    center = Offset(
                        x = size.width / 2f + d.x,
                        y = size.height / 2f + d.y
                    )
                )
            }

            // Boutons Pause/Quit visibles quand on joue
            if (ui.phase == GamePhase.Running) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = { onEvent(GameIntent.Pause) }) { Text("Pause") }
                    Button(onClick = { onEvent(GameIntent.Quit) }) { Text("Quitter") }
                }
            }

            // Overlay Pause
            if (ui.phase == GamePhase.Paused) {
                PauseOverlay(
                    onResume = { onEvent(GameIntent.Resume) },
                    onRestart = {
                        onEvent(GameIntent.Quit)   // Idle
                        onEvent(GameIntent.Start)  // relance propre
                    },
                    onSettings = { onOpenSettings?.invoke() ?: onEvent(GameIntent.Resume) },
                    onQuit = {
                        onEvent(GameIntent.Quit)
                    }
                )
            }
        }
    }
}
