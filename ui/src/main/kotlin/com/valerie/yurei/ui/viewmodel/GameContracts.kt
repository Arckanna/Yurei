package com.valerie.yurei.ui.viewmodel

import androidx.compose.ui.geometry.Offset

enum class GamePhase { Idle, Running, Paused, GameOver }

data class DragonSegmentVM(
    val position: Offset,
    val light: Float
)

data class DragonVM(
    val segments: List<DragonSegmentVM> = emptyList(),
    val headRadius: Float = 24f
)

data class SoulVM(
    val position: Offset,
    val radius: Float,
    val lightIntensity: Float
)

data class FogParticleVM(
    val position: Offset,
    val size: Float,
    val opacity: Float
)

data class GameUiState(
    val phase: GamePhase = GamePhase.Idle,
    val score: Int = 0,
    val dragon: DragonVM = DragonVM(),
    val souls: List<SoulVM> = emptyList(),
    val fogParticles: List<FogParticleVM> = emptyList(),
    val fps: Int = 60
)

sealed class GameIntent {
    data object Start : GameIntent()
    data object Pause : GameIntent()
    data object Resume : GameIntent()
    data object Quit : GameIntent()
    data class Drag(val dx: Float, val dy: Float) : GameIntent()
}
