package com.valerie.yurei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerie.yurei.core.world.LanternJourney
import com.valerie.yurei.ui.components.PauseOverlay
import com.valerie.yurei.ui.viewmodel.*
import kotlinx.coroutines.flow.StateFlow

private val Ink = Color(0xFF101D30)
private val Paper = Color(0xFFF0E7D5)
private val Gold = Color(0xFFE6C68A)

@Composable
fun GameScreen(
    state: StateFlow<GameUiState>,
    onEvent: (GameIntent) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onUpdateWorldSize: ((Size) -> Unit)? = null
) {
    val ui by state.collectAsState()
    BackHandler {
        onEvent(when (ui.phase) {
            GamePhase.Running -> GameIntent.Pause
            GamePhase.Paused -> GameIntent.Resume
            else -> GameIntent.Quit
        })
    }
    if (ui.phase == GamePhase.Completed) {
        MemoryScreen(onHome = { onEvent(GameIntent.Quit) }, onReplay = { onEvent(GameIntent.Start) })
        return
    }
    Box(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("LE LAC DES LANTERNES", color = Gold, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text(LanternJourney.titles[ui.chapter], color = Paper, fontSize = 18.sp)
                }
                Canvas(Modifier.size(68.dp, 24.dp).semantics {
                    contentDescription = "Étape ${ui.chapter + 1} sur 3, ${ui.offerings} lumières recueillies sur 4"
                }) {
                    repeat(3) { i ->
                        drawCircle(if (i <= ui.chapter) Gold else Paper.copy(alpha = 0.2f),
                            radius = if (i == ui.chapter) 5.dp.toPx() else 3.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width * (i + 0.5f) / 3, size.height / 2))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val currentEvent by rememberUpdatedState(onEvent)
                Canvas(Modifier.fillMaxSize()
                    .onSizeChanged { onUpdateWorldSize?.invoke(Size(it.width.toFloat(), it.height.toFloat())) }
                    .semantics { contentDescription = "Dragon spectral. Glissez pour recueillir les lumières. La lanterne est un refuge." }
                    .then(if (ui.phase == GamePhase.Running) Modifier.pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            currentEvent(GameIntent.Drag(drag.x, drag.y))
                        }
                    } else Modifier)) {
                    drawLanternWorld(ui)
                }
                if (ui.phase == GamePhase.Running && ui.chapterMs < 6_000 && ui.chapter > 0) {
                    Text("Un écho se rapproche…", color = Paper, modifier = Modifier
                        .align(Alignment.TopCenter).padding(top = 24.dp), fontSize = 16.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                if (ui.phase == GamePhase.Running) {
                    TextButton(onClick = { onEvent(GameIntent.Pause) }) { Text("Pause", color = Paper) }
                    val light = ui.dragon.segments.firstOrNull()?.light ?: 0.8f
                    Canvas(Modifier.size(110.dp, 28.dp).semantics {
                        contentDescription = "Lumière ${(light * 100).toInt()} pour cent" +
                            if (ui.sheltered) ", refuge" else ""
                    }) {
                        drawLine(Paper.copy(alpha = 0.15f), androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                            androidx.compose.ui.geometry.Offset(size.width, size.height / 2), 3.dp.toPx())
                        drawLine(if (ui.sheltered) Gold else Paper, androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                            androidx.compose.ui.geometry.Offset(size.width * light, size.height / 2), 3.dp.toPx())
                    }
                    TextButton(onClick = { onEvent(GameIntent.Quit) }) { Text("Quitter", color = Paper) }
                } else Spacer(Modifier.height(48.dp))
            }
        }
        when (ui.phase) {
            GamePhase.Intro -> JourneyIntro { onEvent(GameIntent.Begin) }
            GamePhase.Paused -> PauseOverlay(
                onResume = { onEvent(GameIntent.Resume) },
                onRestart = { onEvent(GameIntent.Start) },
                onSettings = { onOpenSettings?.invoke() },
                onQuit = { onEvent(GameIntent.Quit) }
            )
            GamePhase.Idle, GamePhase.GameOver -> if (ui.ready) {
                Button(onClick = { onEvent(GameIntent.Start) }, modifier = Modifier.align(Alignment.Center)) {
                    Text("Commencer la partie")
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun JourneyIntro(onBegin: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("一  /  PREMIER SOUVENIR", color = Gold, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(24.dp))
            Text("Le lac des\nlanternes", color = Paper, fontSize = 36.sp, lineHeight = 44.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("Au-delà de la brume, une lumière vous attend.", color = Paper, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("Glissez pour guider le dragon vers les âmes dorées.\n\nRecueillez quatre lumières dans chaque étape. Le lac s’ouvrira à son rythme.\n\nPrès de la grande lanterne, vous pouvez vous reposer : votre lumière se régénère.",
                color = Paper.copy(alpha = 0.8f), fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("3–5 MINUTES · À VOTRE RYTHME", color = Gold, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onBegin, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                Text("Entrer dans la brume", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun MemoryScreen(onHome: () -> Unit, onReplay: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink).systemBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 28.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SOUVENIR RETROUVÉ", color = Gold, fontSize = 12.sp, letterSpacing = 3.sp)
        Canvas(Modifier.fillMaxWidth().height(260.dp).semantics {
            contentDescription = "Sur la rive, une petite silhouette offre une lanterne au dragon. Leurs reflets se rejoignent dans l’eau."
        }) { drawLanternMemory() }
        Text("La lanterne offerte", color = Paper, fontSize = 30.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text("Avant la brume, quelqu’un vous attendait sur cette rive.\n\nUne petite main avait levé une lanterne. Vous n’aviez pas besoin de retrouver le chemin seul.",
            color = Paper.copy(alpha = 0.85f), fontSize = 17.sp, lineHeight = 26.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Text("La lumière reçue demeure en vous.", color = Gold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
            Text("Retour au sanctuaire")
        }
        TextButton(onClick = onReplay) { Text("Rejouer ce monde", color = Paper) }
    }
}
