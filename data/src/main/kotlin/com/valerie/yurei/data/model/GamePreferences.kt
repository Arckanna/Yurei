package com.valerie.yurei.data.model

/**
 * Préférences et progression sauvegardées (DataStore).
 */
data class GamePreferences(
    val highScore: Int = 0,
    val memoryUnlocked: Boolean = false,
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true
)
