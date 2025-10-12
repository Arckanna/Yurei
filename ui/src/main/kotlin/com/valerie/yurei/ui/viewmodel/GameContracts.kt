package com.valerie.yurei.ui.viewmodel

enum class GamePhase { Idle, Running, Paused, GameOver }

data class DragonVM(
    val x: Float = 0f,
    val y: Float = 0f,
    val light: Float = 0f
)

data class GameUiState(
    val phase: GamePhase = GamePhase.Idle,
    val score: Int = 0,
    val dragon: DragonVM = DragonVM(),
    val fps: Int = 60   // garde 60 pour coller à ton VM
)

sealed class GameIntent {
    data object Start : GameIntent()
    data object Pause : GameIntent()
    data object Resume : GameIntent()
    data object Quit : GameIntent()
    data class Drag(val dx: Float, val dy: Float) : GameIntent()
}
