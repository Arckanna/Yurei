package com.valerie.yurei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valerie.yurei.core.maze.Direction
import com.valerie.yurei.core.maze.MazeRun
import com.valerie.yurei.core.maze.swipeDirection
import com.valerie.yurei.core.world.GameViewport
import com.valerie.yurei.ui.components.PauseOverlay
import com.valerie.yurei.ui.viewmodel.MazePhase
import com.valerie.yurei.ui.viewmodel.MazeUiState
import kotlinx.coroutines.flow.StateFlow

private val MazeInk = Color(0xFF101D30)
private val MazePaper = Color(0xFFF0E7D5)
private val MazeGoldText = Color(0xFFE6C68A)

/** Distance à parcourir avec le doigt pour qu'un glissement compte. À régler à l'essai. */
private val SwipeThreshold = 24.dp

/**
 * Prototype du lac en labyrinthe (REGLES_LABYRINTHE.md §6, étape 2).
 * La pause reste le seul bouton en jeu (§3.6).
 */
@Composable
fun MazeScreen(
    state: StateFlow<MazeUiState>,
    onBegin: () -> Unit,
    onSwipe: (Direction) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    onOpenSettings: () -> Unit,
    onNextLevel: () -> Unit,
    onChooseLevel: (index: Int) -> Unit,
    onReplayWorld: () -> Unit
) {
    val ui by state.collectAsState()
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    BackHandler {
        when (ui.phase) {
            MazePhase.Running -> onPause()
            MazePhase.Paused -> onResume()
            else -> onQuit()
        }
    }
    if (ui.phase == MazePhase.Memory) {
        // Le souvenir du lac, le même que dans l'ancien défi. Le jardin en labyrinthe
        // n'existe pas encore : pas de bouton « monde suivant » dans l'essai.
        MemoryScreen(level = 1, onHome = onQuit, onReplay = onReplayWorld, showNext = false)
        return
    }
    Box(Modifier.fillMaxSize().background(MazeInk).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            MazeHud(ui)
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    // Hors de la phase de jeu, le ViewModel ignore les glissements.
                    .pointerInput(Unit) { detectSwipes { currentOnSwipe(it) } }
            ) {
                Canvas(Modifier.fillMaxSize().semantics {
                    contentDescription = "Labyrinthe du lac. ${ui.lanterns} lanternes, " +
                        "${ui.souls.size} âmes restantes. Glissez pour guider le dragon."
                }) {
                    if (size.width > 0 && size.height > 0) {
                        val viewport = GameViewport.fit(size)
                        withTransform({
                            translate(viewport.offset.x, viewport.offset.y)
                            scale(viewport.scale, viewport.scale, pivot = Offset.Zero)
                            clipRect(0f, 0f, GameViewport.worldSize.width, GameViewport.worldSize.height)
                        }) {
                            drawLakeMaze(ui)
                        }
                    }
                }
            }
            // L'aide s'affiche sous le labyrinthe, pour ne jamais cacher le dragon ni une âme.
            val hint = when {
                ui.waitingForFirstSwipe -> "Tableau ${ui.levelNumber} · glissez pour réveiller le dragon"
                ui.bitten -> "Une lanterne s’éteint · glissez pour repartir"
                else -> null
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (ui.phase == MazePhase.Running) {
                    TextButton(onClick = onPause) { Text("Pause", color = MazePaper) }
                } else Spacer(Modifier.height(48.dp))
                if (hint != null) {
                    Text(hint, color = MazeGoldText, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f))
                }
            }
        }
        when (ui.phase) {
            MazePhase.Intro -> MazeIntro(ui, onChooseLevel, onBegin)
            MazePhase.Paused -> PauseOverlay(onResume = onResume, onRestart = onRestart, onSettings = onOpenSettings, onQuit = onQuit)
            MazePhase.Won -> MazeEnd(
                title = "Tableau ${ui.levelNumber} réussi",
                lines = listOf(
                    "Âmes recueillies : ${ui.totalSouls} × ${MazeRun.SOUL_POINTS}",
                    "Tableau : ${MazeRun.LEVEL_POINTS}",
                    "Bonus de temps : ${MazeRun.timeBonus(ui.elapsedMs)}",
                    "Score du tableau : ${ui.score}",
                    "Score du lac : ${ui.worldScore}"
                ),
                primary = "Tableau ${ui.levelNumber + 1}" to onNextLevel,
                secondary = "Rejouer ce tableau" to onRestart,
                onQuit = onQuit
            )
            MazePhase.Lost -> MazeEnd(
                title = "Les lanternes se sont éteintes",
                lines = listOf("${ui.collected} âmes recueillies sur ${ui.totalSouls}",
                    "Le tableau ${ui.levelNumber} recommence avec ${MazeRun.START_LANTERNS} lanternes."),
                primary = "Recommencer" to onRestart,
                onQuit = onQuit
            )
            MazePhase.Running, MazePhase.Memory -> Unit
        }
    }
}

/** Affichage discret : tableau, lanternes, âmes restantes, bonus de temps (§3.6). */
@Composable
private fun MazeHud(ui: MazeUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("LE LAC DES LANTERNES", color = MazeGoldText, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("Tableau ${ui.levelNumber} / ${ui.levelCount} · essai", color = MazePaper, fontSize = 18.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Canvas(Modifier.size(66.dp, 18.dp).semantics { contentDescription = "${ui.lanterns} lanternes sur ${MazeRun.START_LANTERNS}" }) {
                repeat(MazeRun.START_LANTERNS) { i ->
                    val lit = i < ui.lanterns
                    val centre = Offset(size.height / 2 + i * size.width / 3f, size.height / 2)
                    drawCircle(MazeGoldText.copy(alpha = if (lit) 0.95f else 0.2f), size.height * 0.38f, centre)
                }
            }
            Text("${ui.souls.size} âmes · bonus ${ui.timeBonus}", color = MazePaper, fontSize = 14.sp)
        }
    }
}

@Composable
private fun MazeIntro(ui: MazeUiState, onChooseLevel: (Int) -> Unit, onBegin: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MazeInk.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("一  /  ESSAI DU LABYRINTHE", color = MazeGoldText, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(24.dp))
            Text("Le lac des\nlanternes", color = MazePaper, fontSize = 36.sp, lineHeight = 44.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text(
                "Glissez le doigt vers le haut, le bas, la gauche ou la droite : le dragon tourne au prochain passage libre.\n\n" +
                    "Recueillez toutes les âmes du lac. Chacune allonge son corps.\n\n" +
                    "Le rocher vous arrête sans vous blesser. Mordre votre queue éteint une lanterne ; " +
                    "à la troisième, le tableau recommence.",
                color = MazePaper.copy(alpha = 0.85f), fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text("TROIS TABLEAUX · TOUTES LES ÂMES · 3 LANTERNES · FINIR VITE RAPPORTE PLUS", color = MazeGoldText, fontSize = 11.sp,
                letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            // Réservé à l'essai : commencer directement au tableau voulu.
            Text("Essai : tableau de départ", color = MazePaper.copy(alpha = 0.7f), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(ui.levelCount) { index ->
                    FilterChip(
                        selected = ui.levelNumber == index + 1,
                        onClick = { onChooseLevel(index) },
                        label = { Text("${index + 1}") },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = MazePaper,
                            selectedContainerColor = MazeGoldText,
                            selectedLabelColor = MazeInk
                        )
                    )
                }
            }
            Text("Tableau ${ui.levelNumber} : ${ui.totalSouls} âmes · ${ui.cellsPerSecond.toInt()} cases par seconde",
                color = MazePaper.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onBegin, colors = ButtonDefaults.buttonColors(containerColor = MazeGoldText, contentColor = MazeInk)) {
                Text("Entrer dans le labyrinthe", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun MazeEnd(
    title: String,
    lines: List<String>,
    primary: Pair<String, () -> Unit>,
    onQuit: () -> Unit,
    secondary: Pair<String, () -> Unit>? = null
) {
    Column(
        Modifier.fillMaxSize().background(MazeInk.copy(alpha = 0.94f)).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = MazeGoldText, fontSize = 28.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        lines.forEach { Text(it, color = MazePaper, fontSize = 16.sp, modifier = Modifier.padding(vertical = 3.dp)) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = primary.second) { Text(primary.first) }
        secondary?.let { (label, action) -> OutlinedButton(onClick = action) { Text(label, color = MazePaper) } }
        TextButton(onClick = onQuit) { Text("Retour au sanctuaire", color = MazePaper) }
    }
}

/**
 * Glissements du doigt : dès que le doigt a parcouru [SwipeThreshold], la direction
 * est envoyée, puis la mesure repart de là. Un seul geste en « L » peut donc
 * donner deux virages, sans lever le doigt.
 */
private suspend fun PointerInputScope.detectSwipes(onSwipe: (Direction) -> Unit) {
    val threshold = SwipeThreshold.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var origin = down.position
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val delta = change.position - origin
            swipeDirection(delta.x, delta.y, threshold)?.let { direction ->
                onSwipe(direction)
                origin = change.position
            }
            change.consume()
        }
    }
}
