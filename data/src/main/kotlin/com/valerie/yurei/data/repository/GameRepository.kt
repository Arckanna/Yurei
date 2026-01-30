package com.valerie.yurei.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.valerie.yurei.data.model.GamePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_prefs")

class GameRepository(private val context: Context) {

    private object Keys {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
    }

    val preferences: Flow<GamePreferences> = context.dataStore.data.map { prefs ->
        GamePreferences(
            highScore = prefs[Keys.HIGH_SCORE] ?: 0,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            sfxEnabled = prefs[Keys.SFX_ENABLED] ?: true
        )
    }

    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIGH_SCORE] ?: 0
            if (score > current) {
                prefs[Keys.HIGH_SCORE] = score
            }
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MUSIC_ENABLED] = enabled
        }
    }

    suspend fun setSfxEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SFX_ENABLED] = enabled
        }
    }
}
