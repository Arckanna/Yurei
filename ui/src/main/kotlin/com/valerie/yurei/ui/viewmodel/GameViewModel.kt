package com.valerie.yurei.ui.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valerie.yurei.audio.AudioManager
import com.valerie.yurei.audio.Sfx
import com.valerie.yurei.core.engine.GameLoop
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.World
import com.valerie.yurei.data.repository.GameRepository
import com.valerie.yurei.ui.navigation.RootNav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    private val nav: RootNav
) : AndroidViewModel(application) {

    private val audioManager = AudioManager(application)
    private val gameRepository = GameRepository(application)

    private var dragon: Dragon? = null
    private var world: World? = null
    private var worldSize: Size = Size(1080f, 1920f)

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    private var frameCount = 0
    private var lastFpsUpdate = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            gameRepository.preferences
                .catch { }
                .collect { prefs ->
                    _state.update { it.copy(highScore = prefs.highScore) }
                    audioManager.setMusicEnabled(prefs.musicEnabled)
                    audioManager.setSfxEnabled(prefs.sfxEnabled)
                }
        }
    }

    private val loop = GameLoop(viewModelScope) { dt ->
        val d = dragon ?: return@GameLoop
        val w = world ?: return@GameLoop

        w.update(dt)

        val currentLight = d.light
        val lightDecay = 0.0001f * dt
        d.setLight((currentLight - lightDecay).coerceAtLeast(0f))

        val collectedSoulIds = w.checkSoulCollisions(d.headPosition, d.getHeadRadius())
        if (collectedSoulIds.isNotEmpty()) {
            collectedSoulIds.forEach { _ ->
                d.addSegment()
                d.setLight((d.light + 0.2f).coerceIn(0f, 1f))
            }
            _state.update { it.copy(score = it.score + collectedSoulIds.size * 10) }
            audioManager.playSfx(Sfx.SoulCollect)
        }

        val segments = d.getSegments().map { segment ->
            DragonSegmentVM(segment.position, segment.light)
        }
        val souls = w.getSouls().map { soul ->
            SoulVM(
                position = soul.position,
                radius = soul.getCurrentRadius(w.getTimeMs()),
                lightIntensity = soul.lightIntensity
            )
        }
        val fogParticles = w.getFogParticles().map { fog ->
            FogParticleVM(
                position = fog.position,
                size = fog.size,
                opacity = fog.opacity
            )
        }

        frameCount++
        val now = System.currentTimeMillis()
        val fps = if (now - lastFpsUpdate >= 1000) {
            val calculatedFps = (frameCount * 1000f / (now - lastFpsUpdate)).toInt()
            frameCount = 0
            lastFpsUpdate = now
            calculatedFps
        } else {
            _state.value.fps
        }

        _state.update { u ->
            u.copy(
                phase = if (u.phase == GamePhase.Paused || u.phase == GamePhase.GameOver)
                    u.phase else GamePhase.Running,
                dragon = DragonVM(segments, d.getHeadRadius()),
                souls = souls,
                fogParticles = fogParticles,
                fps = fps
            )
        }

        if (d.light <= 0f && _state.value.phase == GamePhase.Running) {
            stopInternal(gameOver = true)
            nav.toHome()
        }
    }

    fun updateWorldSize(size: Size) {
        worldSize = size
        world?.updateSize(size)
    }

    fun event(intent: GameIntent) {
        when (intent) {
            is GameIntent.Start -> {
                if (_state.value.phase == GamePhase.Idle || _state.value.phase == GamePhase.GameOver) {
                    resetWorld()
                    nav.toGame()
                    startInternal()
                }
            }
            is GameIntent.Pause -> {
                if (_state.value.phase == GamePhase.Running) pauseInternal()
            }
            is GameIntent.Resume -> {
                if (_state.value.phase == GamePhase.Paused) resumeInternal()
            }
            is GameIntent.Quit -> {
                stopInternal(gameOver = false)
                nav.toHome()
            }
            is GameIntent.Drag -> {
                val d = dragon ?: return
                val currentPos = d.headPosition
                d.updateHeadPosition(
                    Offset(
                        currentPos.x + intent.dx,
                        currentPos.y + intent.dy
                    )
                )
            }
        }
    }

    fun onDrag(dx: Float, dy: Float) = event(GameIntent.Drag(dx, dy))
    fun start() = event(GameIntent.Start)
    fun pause() = event(GameIntent.Pause)
    fun resume() = event(GameIntent.Resume)
    fun quit() = event(GameIntent.Quit)

    private fun resetWorld() {
        val centerX = worldSize.width / 2f
        val centerY = worldSize.height / 2f
        dragon = Dragon(Offset(centerX, centerY), initialLength = 1, initialLight = 0.5f)
        world = World(worldSize, initialSoulCount = 5)
        frameCount = 0
        lastFpsUpdate = System.currentTimeMillis()
        _state.update { it.copy(phase = GamePhase.Idle, score = 0) }
    }

    private fun startInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        loop.start()
        audioManager.prepareMusic("ambient")
        audioManager.startMusic()
    }

    private fun pauseInternal() {
        loop.stop()
        _state.update { it.copy(phase = GamePhase.Paused) }
        audioManager.pauseMusic()
        audioManager.playSfx(Sfx.Pause)
    }

    private fun resumeInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        loop.start()
        audioManager.playSfx(Sfx.Resume)
        audioManager.startMusic()
    }

    private fun stopInternal(gameOver: Boolean) {
        loop.stop()
        val score = _state.value.score
        viewModelScope.launch {
            gameRepository.updateHighScore(score)
        }
        if (gameOver) {
            audioManager.playSfx(Sfx.GameOver)
        }
        audioManager.pauseMusic()
        _state.update { it.copy(phase = if (gameOver) GamePhase.GameOver else GamePhase.Idle) }
    }

    override fun onCleared() {
        loop.stop()
        audioManager.release()
        super.onCleared()
    }
}
