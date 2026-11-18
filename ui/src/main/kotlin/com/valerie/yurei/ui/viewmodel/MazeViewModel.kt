package com.valerie.yurei.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valerie.yurei.core.engine.GameLoop
import com.valerie.yurei.core.maze.Cell
import com.valerie.yurei.core.maze.Direction
import com.valerie.yurei.core.maze.GridPoint
import com.valerie.yurei.core.maze.MazeCatalog
import com.valerie.yurei.core.maze.MazeLevel
import com.valerie.yurei.core.maze.MazePlan
import com.valerie.yurei.core.maze.MazeRun
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class MazePhase { Intro, Running, Paused, Won, Lost }

/** Ce que l'écran du labyrinthe affiche ; tout vient de [MazeRun]. */
data class MazeUiState(
    val plan: MazePlan,
    val phase: MazePhase,
    val body: List<GridPoint>,
    val heading: Direction,
    val souls: Set<Cell>,
    val lanterns: Int,
    val moving: Boolean,
    val translucent: Boolean,
    val elapsedMs: Long,
    val timeBonus: Int,
    val score: Int,
    /** Horloge des animations décoratives (âmes qui respirent), en temps de jeu. */
    val animationMs: Long
) {
    val totalSouls get() = plan.souls.size
    val collected get() = totalSouls - souls.size
    /** Le dragon attend encore le premier glissement. */
    val waitingForFirstSwipe get() = phase == MazePhase.Running && !moving && elapsedMs == 0L
    /** Arrêté après une morsure : protégé, il attend un glissement. */
    val bitten get() = phase == MazePhase.Running && !moving && translucent
}

/**
 * Prototype du labyrinthe (étape 2 de REGLES_LABYRINTHE.md §6) : un seul tableau,
 * le lac, sans sauvegarde (elle viendra avec la v8, à l'étape 3).
 *
 * Volontairement séparé de [GameViewModel] : l'ancien défi et sa sauvegarde v7
 * restent intacts tant que le labyrinthe n'est qu'un essai.
 */
class MazeViewModel : ViewModel() {
    private var level: MazeLevel = MazeCatalog.lake1
    private var run = MazeRun(level)
    private var phase = MazePhase.Intro
    private var animationMs = 0L

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<MazeUiState> = _state

    // La boucle tourne seulement en phase Running ; dt est plafonné à 50 ms par GameLoop.
    private val loop = GameLoop(viewModelScope) { dt, _ -> tick(dt) }

    /** Nouveau tableau, avec l'introduction (depuis l'accueil). */
    fun open() {
        loop.stop()
        run = MazeRun(level)
        phase = MazePhase.Intro
        animationMs = 0L
        publish()
    }

    /** Fin de l'introduction : le dragon attend le premier glissement. */
    fun begin() {
        if (phase != MazePhase.Intro) return
        phase = MazePhase.Running
        publish()
        loop.start()
    }

    fun swipe(direction: Direction) {
        if (phase != MazePhase.Running) return
        run.swipe(direction)
        publish()
    }

    fun pause() {
        if (phase != MazePhase.Running) return
        loop.stop()
        phase = MazePhase.Paused
        publish()
    }

    fun resume() {
        if (phase != MazePhase.Paused) return
        phase = MazePhase.Running
        publish()
        loop.start()
    }

    /** Recommence le tableau depuis le début, sans repasser par l'introduction. */
    fun restart() {
        loop.stop()
        run = MazeRun(level)
        phase = MazePhase.Running
        publish()
        loop.start()
    }

    /** Quitte l'écran : plus aucune boucle ne tourne. */
    fun quit() {
        loop.stop()
        if (phase == MazePhase.Running) phase = MazePhase.Paused
        publish()
    }

    private fun tick(dtMs: Long) {
        if (phase != MazePhase.Running) return
        animationMs += dtMs
        run.update(dtMs)
        if (run.finished) {
            phase = if (run.won) MazePhase.Won else MazePhase.Lost
            loop.stop()
        }
        publish()
    }

    private fun publish() {
        _state.value = snapshot()
    }

    private fun snapshot() = MazeUiState(
        plan = level.plan,
        phase = phase,
        body = run.bodyPoints(),
        heading = run.heading,
        souls = run.remainingSouls,
        lanterns = run.lanterns,
        moving = run.moving,
        translucent = run.translucent,
        elapsedMs = run.elapsedMs,
        timeBonus = run.currentTimeBonus(),
        score = run.score,
        animationMs = animationMs
    )

    override fun onCleared() {
        loop.stop()
        super.onCleared()
    }
}
