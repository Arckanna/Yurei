package com.valerie.yurei.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valerie.yurei.core.engine.GameLoop
import com.valerie.yurei.ui.navigation.RootNav
import com.valerie.yurei.data.model.DragonState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(DragonState())
    val state: StateFlow<DragonState> = _state


    private val loop = GameLoop(viewModelScope) { dt ->
        _state.update { s ->
            s.copy(light = (s.light + 0.0005f * dt).coerceIn(0f, 1f))
        }
    }


    fun start() = loop.start()
    fun stop() = loop.stop()


    fun onDrag(dx: Float, dy: Float) {
        _state.update { s ->
            s.copy(positionX = s.positionX + dx, positionY = s.positionY + dy)
        }
    }
}