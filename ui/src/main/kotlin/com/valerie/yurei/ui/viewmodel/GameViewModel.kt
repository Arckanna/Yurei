package com.valerie.yurei.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valerie.yurei.core.engine.GameLoop
import com.valerie.yurei.ui.navigation.RootNav
import com.valerie.yurei.data.model.DragonState

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class GamePhase { Idle, Running, Paused, GameOver }

data class DragonVM(val x: Float = 0f, val y: Float = 0f, val light: Float = 0f)

data class GameUiState(
    val phase: GamePhase = GamePhase.Idle,
    val dragon: DragonVM = DragonVM(),
    val fps: Int = 60,
    val score: Int = 0
)

sealed class GameIntent {
    data object Start : GameIntent()
    data object Pause : GameIntent()
    data object Resume : GameIntent()
    data object Quit : GameIntent()
    data class Drag(val dx: Float, val dy: Float) : GameIntent()
}

/**
 * ViewModel orchestrateur : relie le moteur (core) à l'UI (Compose),
 * gère les intents et la navigation RootNav.
 */
class GameViewModel(
    private val nav: RootNav
) : ViewModel() {

    // NOTE: on garde DragonState en interne si tu l’utilises déjà côté data/model
    private val dragon = MutableStateFlow(DragonState())

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    // Boucle de jeu ~60 FPS (diagramme “tick 60 FPS”)
    private val loop = GameLoop(viewModelScope) { dt ->
        // Update “logique” minimale (ici, simple variation de lumière + position)
        dragon.update { s ->
            s.copy(light = (s.light + 0.0005f * dt).coerceIn(0f, 1f))
        }

        // Projection modèle -> UI
        val d = dragon.value
        _state.update { u ->
            u.copy(
                phase = if (u.phase == GamePhase.Paused || u.phase == GamePhase.GameOver) u.phase else GamePhase.Running,
                dragon = DragonVM(d.positionX, d.positionY, d.light),
                // tu pourras remplacer ce fps “indicatif” par une vraie mesure si besoin
                fps = 60
            )
        }

        // Exemple de condition de fin (à adapter) : si light tombe à 0
        if (_state.value.dragon.light <= 0f && _state.value.phase == GamePhase.Running) {
            stopInternal(gameOver = true)
            nav.toHome() // retour menu après game over; ou affiche un dialog
        }
    }

    // ---------- Cycle de vie / transitions ----------
    fun event(intent: GameIntent) {
        when (intent) {
            is GameIntent.Start -> {
                if (_state.value.phase == GamePhase.Idle || _state.value.phase == GamePhase.GameOver) {
                    resetWorld()
                    nav.toGame()
                    startInternal()
                }
            }
            is GameIntent.Pause -> {
                if (_state.value.phase == GamePhase.Running) {
                    pauseInternal()
                    // Ici, si tu as un dialog Pause : ouvre-le côté UI
                    // Sinon route dédiée : nav.toSettings() ou nav.back()
                }
            }
            is GameIntent.Resume -> {
                if (_state.value.phase == GamePhase.Paused) {
                    resumeInternal()
                }
            }
            is GameIntent.Quit -> {
                stopInternal(gameOver = false)
                nav.toHome()
            }
            is GameIntent.Drag -> {
                // Input → mise à jour du modèle
                dragon.update { s -> s.copy(positionX = s.positionX + intent.dx, positionY = s.positionY + intent.dy) }
            }
        }
    }

    // Ex utilités si tu veux conserver tes méthodes actuelles
    fun onDrag(dx: Float, dy: Float) = event(GameIntent.Drag(dx, dy))
    fun start() = event(GameIntent.Start)
    fun pause() = event(GameIntent.Pause)
    fun resume() = event(GameIntent.Resume)
    fun quit() = event(GameIntent.Quit)

    // ---------- Privé : helpers ----------
    private fun resetWorld() {
        dragon.value = DragonState() // remet à zéro ton modèle (score, pos, etc. si besoin)
        _state.value = GameUiState(phase = GamePhase.Idle)
    }

    private fun startInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        loop.start()
    }

    private fun pauseInternal() {
        loop.stop()
        _state.update { it.copy(phase = GamePhase.Paused) }
    }

    private fun resumeInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        loop.start()
    }

    private fun stopInternal(gameOver: Boolean) {
        loop.stop()
        _state.update { it.copy(phase = if (gameOver) GamePhase.GameOver else GamePhase.Idle) }
    }

    override fun onCleared() {
        loop.stop()
        super.onCleared()
    }
}
