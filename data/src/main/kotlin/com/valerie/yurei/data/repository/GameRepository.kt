package com.valerie.yurei.data.repository

import android.content.Context
import com.valerie.yurei.data.model.SavedGame
import com.valerie.yurei.data.model.SavedGameCodec
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
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

class GameRepository(context: Context) {
    private val context = context.applicationContext

    companion object {
        // Process-owned FIFO: writes survive ViewModel disposal and cannot overtake
        // a new-game deletion. DataStore commits each snapshot atomically.
        private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
        private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            for (write in writes) write()
        }
    }

    suspend fun loadGame(): SavedGame? {
        val barrier = CompletableDeferred<Unit>()
        writes.send { barrier.complete(Unit) }
        barrier.await()
        return context.dataStore.data.first()[Keys.GAME]?.let(SavedGameCodec::decode)
    }

    fun saveGame(game: SavedGame?, onComplete: (Throwable?) -> Unit = {}) {
        writes.trySend {
            val result = runCatching {
                val encoded = game?.let(SavedGameCodec::encode)
                context.dataStore.edit { prefs ->
                    if (encoded == null) prefs.remove(Keys.GAME) else prefs[Keys.GAME] = encoded
                    if (game?.completed == true) {
                        prefs[Keys.MEMORY] = true
                        if (game.level == 2) prefs[Keys.SAKURA_MEMORY] = true
                    }
                    if (game != null && game.score > (prefs[Keys.HIGH_SCORE] ?: 0)) prefs[Keys.HIGH_SCORE] = game.score
                }
            }
            onComplete(result.exceptionOrNull())
        }.getOrThrow()
    }


    private object Keys {
        val SAKURA_MEMORY = booleanPreferencesKey("sakura_memory_unlocked")
        val MEMORY = booleanPreferencesKey("lantern_memory_unlocked")
        val GAME = stringPreferencesKey("saved_game_v1")
        val HIGH_SCORE = intPreferencesKey("high_score")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
    }

    val preferences: Flow<GamePreferences> = context.dataStore.data.map { prefs ->
        GamePreferences(
            highScore = prefs[Keys.HIGH_SCORE] ?: 0,
            memoryUnlocked = prefs[Keys.MEMORY] ?: false,
            sakuraMemoryUnlocked = prefs[Keys.SAKURA_MEMORY] ?: false,
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
