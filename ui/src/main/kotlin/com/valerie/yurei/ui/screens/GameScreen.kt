package com.valerie.yurei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import com.valerie.yurei.core.world.GameViewport
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerie.yurei.core.world.SakuraGarden
import com.valerie.yurei.core.world.LanternJourney
import com.valerie.yurei.ui.components.SteeringJoystick
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
        MemoryScreen(level = ui.level, onHome = { onEvent(GameIntent.Quit) }, onReplay = { onEvent(GameIntent.Start) }, onNext = { onEvent(GameIntent.StartLevel(2)) })
        return
    }
    Box(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(if (ui.level == 2) "LE JARDIN DES PROMESSES" else "LE LAC DES LANTERNES", color = Gold, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text((if (ui.level == 2) SakuraGarden.titles else LanternJourney.titles)[ui.chapter], color = Paper, fontSize = 18.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val seconds = (ui.remainingMs + 999) / 1000
                    Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                        color = if (seconds <= 30) Color(0xFFFFAB91) else Gold, fontSize = 24.sp)
                    Text("${ui.collected} / 30 âmes", color = Paper, fontSize = 14.sp)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val currentEvent by rememberUpdatedState(onEvent)
                Canvas(Modifier.fillMaxSize()
                    .onSizeChanged { onUpdateWorldSize?.invoke(Size(it.width.toFloat(), it.height.toFloat())) }
                    .semantics { contentDescription = "Dragon en mouvement continu. Glissez pour orienter sa course et évitez sa queue." }
                ) {
                    if (size.width > 0 && size.height > 0) {
                        val viewport = GameViewport.fit(size)
                        withTransform({
                            translate(viewport.offset.x, viewport.offset.y)
                            scale(viewport.scale, viewport.scale, pivot = Offset.Zero)
                            clipRect(0f, 0f, GameViewport.worldSize.width, GameViewport.worldSize.height)
                        }) {
                            drawLanternWorld(ui, GameViewport.worldSize)
                        }
                    }
                }
                if (ui.phase == GamePhase.Running) {
                    SteeringJoystick(Modifier.matchParentSize()) { dx, dy ->
                        currentEvent(GameIntent.Drag(dx, dy))
                    }
                }
                if (ui.phase == GamePhase.Running && ui.collisionGraceMs > 0) {
                    Text(when (ui.hitScenery) {
                        true -> "Pierre touchée : −10 s · Protection temporaire"
                        false -> "Queue touchée : −10 s · Protection temporaire"
                        null -> "Collision : −10 s · Protection temporaire"
                    }, color = Paper, modifier = Modifier
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
            GamePhase.Intro -> JourneyIntro(ui.level) { onEvent(GameIntent.Begin) }
            GamePhase.Paused -> PauseOverlay(
                onResume = { onEvent(GameIntent.Resume) },
                onRestart = { onEvent(GameIntent.Start) },
                onSettings = { onOpenSettings?.invoke() },
                onQuit = { onEvent(GameIntent.Quit) }
            )
            GamePhase.GameOver -> Column(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.94f)),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Le temps s’est écoulé", color = Gold, fontSize = 28.sp)
                Text("${ui.collected} âmes recueillies sur 30", color = Paper, modifier = Modifier.padding(24.dp))
                Button(onClick = { onEvent(GameIntent.Start) }) { Text("Réessayer") }
                TextButton(onClick = { onEvent(GameIntent.Quit) }) { Text("Retour au sanctuaire", color = Paper) }
            }
            GamePhase.Idle -> if (ui.ready) {
                Button(onClick = { onEvent(GameIntent.Start) }, modifier = Modifier.align(Alignment.Center)) {
                    Text("Commencer la partie")
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun JourneyIntro(level: Int, onBegin: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (level == 2) "二  /  DEUXIÈME SOUVENIR" else "一  /  PREMIER SOUVENIR", color = Gold, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(24.dp))
            Text(if (level == 2) "Le jardin des\npromesses" else "Le lac des\nlanternes", color = Paper, fontSize = 36.sp, lineHeight = 44.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text(if (level == 2) "Sous les cerisiers, une promesse attend de refleurir." else "Au-delà de la brume, une lumière vous attend.", color = Paper, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("Recueillez 30 âmes avant la fin des trois minutes.\n\nLe dragon avance seul. Posez le doigt, puis écartez-le du centre du joystick pour choisir une direction. Revenez au centre ou relâchez pour conserver le cap. Chaque âme allonge son corps.\n\nToucher votre queue coûte 10 secondes et trois segments. Vous êtes ensuite protégé pendant deux secondes. Les berges vous font rebondir sans pénalité.",
                color = Paper.copy(alpha = 0.8f), fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            if (level == 2) Text("Contournez les trois pierres bordées de rose : les toucher coûte aussi 10 secondes. Les pétales et les arbres sont décoratifs.", color = Gold, textAlign = TextAlign.Center, fontSize = 15.sp)
            Text("30 ÂMES · 3 MINUTES · ÉVITEZ VOTRE QUEUE", color = Gold, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onBegin, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                Text("Entrer dans la brume", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun MemoryScreen(onHome: () -> Unit, onReplay: () -> Unit, level: Int = 1, onNext: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Ink).systemBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 28.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SOUVENIR RETROUVÉ", color = Gold, fontSize = 12.sp, letterSpacing = 3.sp)
        Canvas(Modifier.fillMaxWidth().height(260.dp).semantics {
            contentDescription = if (level == 2) "Sous un cerisier en fleurs, une silhouette offre un pétale au dragon." else "Sur la rive, une petite silhouette offre une lanterne au dragon. Leurs reflets se rejoignent dans l’eau."
        }) { if (level == 2) drawSakuraMemory() else drawLanternMemory() }
        Text(if (level == 2) "La promesse du printemps" else "La lanterne offerte", color = Paper, fontSize = 30.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text(if (level == 2) "Chaque printemps, vous reveniez sous cet arbre. Une petite main déposait un pétale dans votre paume.\n\nMême lorsque les fleurs tombaient, la promesse restait : nous nous retrouverons." else "Avant la brume, quelqu’un vous attendait sur cette rive.\n\nUne petite main avait levé une lanterne. Vous n’aviez pas besoin de retrouver le chemin seul.",
            color = Paper.copy(alpha = 0.85f), fontSize = 17.sp, lineHeight = 26.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Text(if (level == 2) "Ce qui a fleuri en vous ne disparaît pas." else "La lumière reçue demeure en vous.", color = Gold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
            Text("Retour au sanctuaire")
        }
        if (level == 1) Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B0C9), contentColor = Ink)) { Text("Vers le jardin des sakuras") }
        TextButton(onClick = onReplay) { Text("Rejouer ce monde", color = Paper) }
    }
}
