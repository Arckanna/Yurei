package com.valerie.yurei.ui.viewmodel

import androidx.compose.ui.geometry.Offset

enum class GamePhase { Idle, Intro, Running, Paused, Completed, GameOver }

data class DragonSegmentVM(
    val position: Offset,
    val light: Float
)

data class DragonVM(
    val segments: List<DragonSegmentVM> = emptyList(),
    val headRadius: Float = com.valerie.yurei.core.entity.Dragon.HEAD_RADIUS
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
    val level: Int = 1,
    val worldTimeMs: Long = 0,
    val moonMemoryUnlocked: Boolean = false,
    val sakuraMemoryUnlocked: Boolean = false,
    val hitScenery: Boolean? = null,
    val remainingMs: Long = 180_000,
    val collected: Int = 0,
    val collisionGraceMs: Long = 0,
    val chapter: Int = 0,
    val chapterMs: Long = 0,
    val offerings: Int = 0,
    val journeyProgress: Float = 0f,
    val journeyMs: Long = 0,
    val refuge: Offset = Offset.Zero,
    val refugeRadius: Float = 0f,
    val sheltered: Boolean = false,
    val memoryUnlocked: Boolean = false,
    val ready: Boolean = false,
    val hasSavedGame: Boolean = false,
    val persistenceError: String? = null,
    val phase: GamePhase = GamePhase.Idle,
    val score: Int = 0,
    val highScore: Int = 0,
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val dragon: DragonVM = DragonVM(),
    val souls: List<SoulVM> = emptyList(),
    val fogParticles: List<FogParticleVM> = emptyList(),
    val fps: Int = 60
)

sealed class GameIntent {
    data object Continue : GameIntent()
    data object Begin : GameIntent()
    data object Start : GameIntent()
    data class StartLevel(val level: Int) : GameIntent()
    data object Pause : GameIntent()
    data object Resume : GameIntent()
    data object Quit : GameIntent()
    data class Drag(val dx: Float, val dy: Float) : GameIntent()
}
