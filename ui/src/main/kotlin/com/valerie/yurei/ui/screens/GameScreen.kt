package com.valerie.yurei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onOpenSettings: (() -> Unit)? = null,
    onUpdateWorldSize: ((Size) -> Unit)? = null
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
            Text("Score: ${ui.score}", color = Color.White)
            Text("FPS: ${ui.fps}", color = Color.White)
        }

        // Zone de jeu + drag (désactivé quand Paused ou GameOver)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E), // Bleu très foncé
                            Color(0xFF16213E), // Bleu foncé
                            Color(0xFF0F3460)  // Bleu nuit
                        )
                    )
                )
                .then(
                    if (ui.phase == GamePhase.Running) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { _, drag ->
                                onEvent(GameIntent.Drag(drag.x, drag.y))
                            }
                        }
                    } else {
                        Modifier // pas d'input quand en pause
                    }
                )
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Notifier la taille du monde au ViewModel
                onUpdateWorldSize?.invoke(size)

                // 1. Dessiner la brume en arrière-plan
                ui.fogParticles.forEach { fog ->
                    drawCircle(
                        color = Color.White.copy(alpha = fog.opacity),
                        radius = fog.size / 2f,
                        center = fog.position
                    )
                }

                // 2. Dessiner les âmes lumineuses
                ui.souls.forEach { soul ->
                    // Halo extérieur (lumière diffuse)
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = soul.lightIntensity * 0.3f),
                        radius = soul.radius * 1.5f,
                        center = soul.position
                    )
                    // Âme principale
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = soul.lightIntensity),
                        radius = soul.radius,
                        center = soul.position
                    )
                    // Centre brillant
                    drawCircle(
                        color = Color.White.copy(alpha = soul.lightIntensity * 0.8f),
                        radius = soul.radius * 0.5f,
                        center = soul.position
                    )
                }

                // 3. Dessiner le dragon segmenté
                val dragon = ui.dragon
                if (dragon.segments.isNotEmpty()) {
                    // Dessiner les connexions entre segments
                    val segments = dragon.segments
                    
                    for (i in 0 until segments.size - 1) {
                        val current = segments[i].position
                        val next = segments[i + 1].position
                        
                        val path = Path().apply {
                            moveTo(current.x, current.y)
                            lineTo(next.x, next.y)
                        }
                        
                        val segmentLight = segments[i].light
                        drawPath(
                            path = path,
                            color = Color(0xFF64B5F6).copy(alpha = segmentLight),
                            style = Stroke(
                                width = 12f + 8f * segmentLight
                            )
                        )
                    }
                    
                    // Dessiner chaque segment
                    segments.forEachIndexed { index, segment ->
                        val radius = if (index == 0) {
                            // Tête : plus grande
                            dragon.headRadius * segment.light
                        } else {
                            // Corps : taille décroissante
                            (20f + 10f * segment.light) * (1f - index * 0.05f).coerceAtLeast(0.5f)
                        }
                        
                        // Halo lumineux autour du segment
                        drawCircle(
                            color = Color(0xFF64B5F6).copy(alpha = segment.light * 0.2f),
                            radius = radius * 1.8f,
                            center = segment.position
                        )
                        
                        // Segment principal
                        drawCircle(
                            color = Color(0xFF64B5F6).copy(alpha = segment.light),
                            radius = radius,
                            center = segment.position
                        )
                        
                        // Centre brillant (surtout pour la tête)
                        if (index == 0) {
                            drawCircle(
                                color = Color.White.copy(alpha = segment.light * 0.6f),
                                radius = radius * 0.4f,
                                center = segment.position
                            )
                        }
                    }
                }
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
