package com.valerie.yurei.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

/**
 * Gestion audio : SoundPool pour les FX courts, ExoPlayer pour la musique d'ambiance.
 * Les FX sont chargés par nom depuis le package de l'app (res/raw/).
 */
class AudioManager(
    private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<Sfx, Int>()
    private var musicPlayer: ExoPlayer? = null

    var sfxEnabled: Boolean = true
        private set
    var musicEnabled: Boolean = true
        private set

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        Sfx.values().forEach { sfx ->
            val resId = context.resources.getIdentifier(sfx.rawName, "raw", context.packageName)
            if (resId != 0) {
                soundIds[sfx] = soundPool!!.load(context, resId, 1)
            }
        }
    }

    /**
     * Joue un effet sonore court (collecte d'âme, pause, etc.).
     */
    fun playSfx(sfx: Sfx) {
        if (!sfxEnabled) return
        val id = soundIds[sfx] ?: return
        soundPool?.play(id, 0.7f, 0.7f, 1, 0, 1f)
    }

    /**
     * Active ou désactive les effets sonores.
     */
    fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
    }

    /**
     * Active ou désactive la musique.
     */
    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) {
            musicPlayer?.pause()
        }
    }

    /**
     * Prépare la musique d'ambiance (raw ou assets).
     * resourceName = nom sans extension dans res/raw (ex. "ambient" → res/raw/ambient.mp3).
     */
    fun prepareMusic(resourceName: String, loop: Boolean = true) {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resId == 0) return
        if (musicPlayer == null) {
            musicPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            }
        }
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        musicPlayer?.setMediaItem(MediaItem.fromUri(uri))
        musicPlayer?.prepare()
    }

    /**
     * Démarre la musique (si préparée et musicEnabled).
     */
    fun startMusic() {
        if (!musicEnabled) return
        musicPlayer?.playWhenReady = true
    }

    /**
     * Met la musique en pause.
     */
    fun pauseMusic() {
        musicPlayer?.playWhenReady = false
    }

    /**
     * Libère les ressources (à appeler en onCleared ou onDestroy).
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        musicPlayer?.release()
        musicPlayer = null
    }
}
