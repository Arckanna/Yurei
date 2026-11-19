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

/**
 * Intro : présentation du monde. Won : un tableau réussi (il en reste d'autres).
 * Memory : le dernier tableau du monde est réussi, le souvenir s'affiche.
 */
enum class MazePhase { Intro, Running, Paused, Won, Lost, Memory }

/** Ce que l'écran du labyrinthe affiche ; tout vient de [MazeRun]. */
data class MazeUiState(
    val plan: MazePlan,
    val phase: MazePhase,
    /** Numéro du tableau dans le monde, à partir de 1. */
    val levelNumber: Int,
    val levelCount: Int,
    val cellsPerSecond: Float,
    val body: List<GridPoint>,
    val heading: Direction,
    val souls: Set<Cell>,
    val lanterns: Int,
    val moving: Boolean,
    val translucent: Boolean,
    val elapsedMs: Long,
    val timeBonus: Int,
    /** Score du tableau en cours. */
    val score: Int,
    /** Somme des tableaux déjà réussis du monde, plus le tableau en cours une fois gagné. */
    val worldScore: Int,
    /** Horloge des animations décoratives (âmes qui respirent), en temps de jeu. */
    val animationMs: Long
) {
    val totalSouls get() = plan.souls.size
    val collected get() = totalSouls - souls.size
    val isLastLevel get() = levelNumber == levelCount
    /** Le dragon attend encore le premier glissement. */
    val waitingForFirstSwipe get() = phase == MazePhase.Running && !moving && elapsedMs == 0L
    /** Arrêté après une morsure : protégé, il attend un glissement. */
    val bitten get() = phase == MazePhase.Running && !moving && translucent
}

/**
 * Essai du labyrinthe (REGLES_LABYRINTHE.md §6, étapes 2 et 4) : les trois tableaux
 * du lac s'enchaînent, puis le souvenir. Sans sauvegarde : elle viendra avec la v8
 * (étape 3), avec le record par monde et le déblocage du monde suivant.
 *
 * Volontairement séparé de [GameViewModel] : l'ancien défi et sa sauvegarde v7
 * restent intacts tant que le labyrinthe n'est qu'un essai.
 */
class MazeViewModel : ViewModel() {
    private val levels: List<MazeLevel> = MazeCatalog.lake
    private var levelIndex = 0
    private var run = MazeRun(levels[levelIndex])
    private var phase = MazePhase.Intro
    private var animationMs = 0L
    /** Points des tableaux déjà réussis dans ce passage du monde. */
    private var earlierLevelsScore = 0

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<MazeUiState> = _state

    // La boucle tourne seulement en phase Running ; dt est plafonné à 50 ms par GameLoop.
    private val loop = GameLoop(viewModelScope) { dt, _ -> tick(dt) }

    /** Ouvre le monde depuis l'accueil : introduction, tableau 1. */
    fun open() {
        loop.stop()
        earlierLevelsScore = 0
        loadLevel(0, MazePhase.Intro)
    }

    /**
     * Pendant l'introduction, choisit le tableau de départ. Réservé à l'essai, pour
     * tester un tableau sans rejouer les précédents ; la progression le remplacera.
     */
    fun chooseLevel(index: Int) {
        if (phase != MazePhase.Intro || index !in levels.indices) return
        earlierLevelsScore = 0
        loadLevel(index, MazePhase.Intro)
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

    /**
     * Recommence le tableau en cours depuis le début, avec 3 lanternes (§3.4).
     * Les tableaux déjà réussis restent acquis.
     */
    fun restart() {
        loop.stop()
        loadLevel(levelIndex, MazePhase.Running)
        loop.start()
    }

    /** Après un tableau réussi : le suivant, sans repasser par l'introduction. */
    fun nextLevel() {
        if (phase != MazePhase.Won || levelIndex + 1 !in levels.indices) return
        earlierLevelsScore += run.score
        loadLevel(levelIndex + 1, MazePhase.Running)
        loop.start()
    }

    /** Depuis le souvenir : rejouer le monde entier, depuis le tableau 1. */
    fun replayWorld() {
        open()
    }

    /** Quitte l'écran : plus aucune boucle ne tourne. */
    fun quit() {
        loop.stop()
        if (phase == MazePhase.Running) phase = MazePhase.Paused
        publish()
    }

    private fun loadLevel(index: Int, newPhase: MazePhase) {
        levelIndex = index
        run = MazeRun(levels[index])
        phase = newPhase
        animationMs = 0L
        publish()
    }

    private fun tick(dtMs: Long) {
        if (phase != MazePhase.Running) return
        animationMs += dtMs
        run.update(dtMs)
        if (run.finished) {
            phase = when {
                run.lost -> MazePhase.Lost
                levelIndex == levels.lastIndex -> MazePhase.Memory
                else -> MazePhase.Won
            }
            loop.stop()
        }
        publish()
    }

    private fun publish() {
        _state.value = snapshot()
    }

    private fun snapshot() = MazeUiState(
        plan = run.level.plan,
        phase = phase,
        levelNumber = levelIndex + 1,
        levelCount = levels.size,
        cellsPerSecond = run.level.cellsPerSecond,
        body = run.bodyPoints(),
        heading = run.heading,
        souls = run.remainingSouls,
        lanterns = run.lanterns,
        moving = run.moving,
        translucent = run.translucent,
        elapsedMs = run.elapsedMs,
        timeBonus = run.currentTimeBonus(),
        score = run.score,
        worldScore = earlierLevelsScore + if (run.won) run.score else 0,
        animationMs = animationMs
    )

    override fun onCleared() {
        loop.stop()
        super.onCleared()
    }
}
