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
    private val musicState = MusicPlaybackState()
    private val loadedSounds = mutableSetOf<Int>()

    var sfxEnabled: Boolean = true
        private set
    val musicEnabled: Boolean get() = musicState.enabled

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
        soundPool?.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) loadedSounds.add(id)
        }
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
        if (id !in loadedSounds) return
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
        if (!musicState.setEnabled(enabled)) return
        musicPlayer?.playWhenReady = musicState.shouldPlay
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
                setAudioAttributes(com.google.android.exoplayer2.audio.AudioAttributes.Builder()
                    .setUsage(com.google.android.exoplayer2.C.USAGE_GAME)
                    .setContentType(com.google.android.exoplayer2.C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                setHandleAudioBecomingNoisy(true)
                addListener(object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (!playWhenReady && (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY ||
                                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)) {
                            musicState.interrupt()
                        }
                    }
                })
                volume = 0.65f
            }
        }
        musicPlayer?.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        musicPlayer?.setMediaItem(MediaItem.fromUri(uri))
        musicPlayer?.prepare()
    }

    /**
     * Démarre la musique (si préparée et musicEnabled).
     */
    fun startMusic(explicitResume: Boolean = false) {
        musicState.start(explicitResume)
        musicPlayer?.playWhenReady = musicState.shouldPlay
    }

    /**
     * Met la musique en pause.
     */
    fun pauseMusic() {
        musicState.pause()
        musicPlayer?.playWhenReady = false
    }

    /**
     * Libère les ressources (à appeler en onCleared ou onDestroy).
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        loadedSounds.clear()
        musicPlayer?.release()
        musicPlayer = null
    }
}
