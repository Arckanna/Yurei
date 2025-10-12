package com.valerie.yurei.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valerie.yurei.core.engine.GameLoop
import com.valerie.yurei.ui.navigation.RootNav
import com.valerie.yurei.data.model.DragonState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GameViewModel(
    private val nav: RootNav
) : ViewModel() {

    private val dragon = MutableStateFlow(DragonState())

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    private val loop = GameLoop(viewModelScope) { dt ->
        dragon.update { s ->
            s.copy(light = (s.light + 0.0005f * dt).coerceIn(0f, 1f))
        }

        val d = dragon.value
        _state.update { u ->
            u.copy(
                phase = if (u.phase == GamePhase.Paused || u.phase == GamePhase.GameOver)
                    u.phase else GamePhase.Running,
                dragon = DragonVM(d.positionX, d.positionY, d.light),
                fps = 60
            )
        }

        if (_state.value.dragon.light <= 0f && _state.value.phase == GamePhase.Running) {
            stopInternal(gameOver = true)
            nav.toHome()
        }
    }

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
                if (_state.value.phase == GamePhase.Running) pauseInternal()
            }
            is GameIntent.Resume -> {
                if (_state.value.phase == GamePhase.Paused) resumeInternal()
            }
            is GameIntent.Quit -> {
                stopInternal(gameOver = false)
                nav.toHome()
            }
            is GameIntent.Drag -> {
                dragon.update { s ->
                    s.copy(
                        positionX = s.positionX + intent.dx,
                        positionY = s.positionY + intent.dy
                    )
                }
            }
        }
    }

    fun onDrag(dx: Float, dy: Float) = event(GameIntent.Drag(dx, dy))
    fun start() = event(GameIntent.Start)
    fun pause() = event(GameIntent.Pause)
    fun resume() = event(GameIntent.Resume)
    fun quit() = event(GameIntent.Quit)

    private fun resetWorld() {
        dragon.value = DragonState()
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
