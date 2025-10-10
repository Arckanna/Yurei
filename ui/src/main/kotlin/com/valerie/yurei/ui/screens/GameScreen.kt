package com.valerie.yurei.ui.screens

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
import com.valerie.yurei.ui.viewmodel.GameIntent
import com.valerie.yurei.ui.viewmodel.GameUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GameScreen(
    state: StateFlow<GameUiState>,
    onEvent: (GameIntent) -> Unit
) {
    val ui by state.collectAsState() // nécessite import androidx.compose.runtime.getValue

    Column(Modifier.fillMaxSize()) {
        // Petit HUD
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Score: ${ui.score}")
            Text("FPS: ${ui.fps}")
        }

        // Zone de jeu + drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        onEvent(GameIntent.Drag(drag.x, drag.y))
                    }
                }
        ) {
            // Dragon minimaliste : un cercle à la position (x, y)
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val d = ui.dragon
                drawCircle(
                    color = Color(0xFF64B5F6),
                    radius = 24f + 24f * d.light, // petit effet avec la lumière
                    center = Offset(
                        x = size.width / 2f + d.x,
                        y = size.height / 2f + d.y
                    )
                )
            }

            // Boutons Pause/Quit rapido (facultatif)
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
    }
}
