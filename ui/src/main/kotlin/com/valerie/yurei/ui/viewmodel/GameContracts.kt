package com.valerie.yurei.ui.viewmodel

enum class GamePhase { Idle, Running, Paused, GameOver }

data class DragonVM(
    val x: Float,
    val y: Float,
    val light: Float
)

data class GameUiState(
    val phase: GamePhase = GamePhase.Idle,
    val score: Int = 0,
    val dragon: DragonVM = DragonVM(0f, 0f, 0f),
    val fps: Int = 0
)

sealed class GameIntent {
    data object Start : GameIntent()
    data object Pause : GameIntent()
    data object Resume : GameIntent()
    data object Quit : GameIntent()
    data class Drag(val dx: Float, val dy: Float) : GameIntent()
}
