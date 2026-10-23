package com.valerie.yurei.ui.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valerie.yurei.audio.AudioManager
import com.valerie.yurei.audio.Sfx
import com.valerie.yurei.core.engine.GameLoop
import com.valerie.yurei.core.engine.LightBalance
import com.valerie.yurei.core.world.Soul
import com.valerie.yurei.core.world.FogParticle
import com.valerie.yurei.data.model.*
import kotlinx.coroutines.CancellationException
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.World
import com.valerie.yurei.core.world.isPlayable
import com.valerie.yurei.data.repository.GameRepository
import com.valerie.yurei.ui.navigation.RootNav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    private var nav: RootNav
) : AndroidViewModel(application) {

    private val audioManager = AudioManager(application)
    private val gameRepository = GameRepository(application)

    private var dragon: Dragon? = null
    private var world: World? = null
    private var worldSize: Size = Size.Zero
    private var viewportSize: Size = Size.Zero

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    private var balance = LightBalance()
    private var checkpointMs = 0L
    private var savedGame: SavedGame? = null
    private var saveRevision = 0L

    private var frameCount = 0
    private var lastFpsUpdate = System.nanoTime() / 1_000_000L

    private val loop = GameLoop(viewModelScope) { dt ->
        if (_state.value.phase != GamePhase.Running) return@GameLoop
        val d = dragon ?: return@GameLoop
        val w = world ?: return@GameLoop

        d.update(dt)
        w.update(dt)

        balance.update(d, dt)

        val collectedSoulIds = w.checkSoulCollisions(d.headPosition, d.getHeadRadius())
        if (collectedSoulIds.isNotEmpty()) {
            collectedSoulIds.forEach { _ ->
                balance.collect(d)
            }
            _state.update { it.copy(score = it.score + collectedSoulIds.size * 10) }
            audioManager.playSfx(Sfx.SoulCollect)
        }

        frameCount++
        val now = System.nanoTime() / 1_000_000L
        val fps = if (now - lastFpsUpdate >= 1000) {
            val calculatedFps = (frameCount * 1000f / (now - lastFpsUpdate)).toInt()
            frameCount = 0
            lastFpsUpdate = now
            calculatedFps
        } else {
            _state.value.fps
        }

        publishWorld(fps)

        checkpointMs += dt
        if (d.light > 0f && (checkpointMs >= 2_000L || collectedSoulIds.isNotEmpty())) checkpoint()
        if (d.light <= 0f && _state.value.phase == GamePhase.Running) {
            stopInternal(gameOver = true)
            nav.toHome()
        }
    }

    init {
        viewModelScope.launch {
            try {
                savedGame = gameRepository.loadGame()
                savedGame?.let { restoreGame(it) }
                _state.update { it.copy(ready = true, hasSavedGame = savedGame != null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update { it.copy(ready = true, persistenceError = "La sauvegarde n’a pas pu être chargée. Vous pouvez commencer une nouvelle partie.") }
            }
        }
        viewModelScope.launch {
            gameRepository.preferences
                .catch { }
                .collect { prefs ->
                    _state.update {
                        it.copy(
                            highScore = prefs.highScore,
                            musicEnabled = prefs.musicEnabled,
                            sfxEnabled = prefs.sfxEnabled
                        )
                    }
                    audioManager.setMusicEnabled(prefs.musicEnabled)
                    audioManager.setSfxEnabled(prefs.sfxEnabled)
                }
        }
    }

    private fun publishWorld(fps: Int = _state.value.fps) {
        val d = dragon ?: return
        val w = world ?: return
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

        _state.update { it.copy(
            dragon = DragonVM(segments, d.getHeadRadius()),
            souls = souls,
            fogParticles = fogParticles,
            fps = fps
        ) }
    }

    // A retained ViewModel must use the current Activity's navigation controller.
    fun updateNavigator(navigator: RootNav) { nav = navigator }

    fun updateWorldSize(size: Size) {
        if (!size.isPlayable()) return
        viewportSize = size
        val previousSize = worldSize
        worldSize = size
        if (world == null) {
            if (_state.value.phase == GamePhase.Running || _state.value.phase == GamePhase.Paused) {
                resetWorld()
                if (_state.value.phase == GamePhase.Running) loop.start()
            }
        } else if (size != previousSize) {
            dragon?.resize(previousSize, size)
            world?.updateSize(size)
            publishWorld()
        }
    }

    fun event(intent: GameIntent) {
        if (!_state.value.ready) return
        when (intent) {
            is GameIntent.Continue -> {
                val saved = savedGame ?: return
                restoreGame(saved)
                nav.toGame()
            }
            is GameIntent.Start -> {
                if (_state.value.phase != GamePhase.Running) {
                    save(null)
                    loop.stop()
                    dragon = null
                    world = null
                    _state.update { it.copy(score = 0, dragon = DragonVM(), souls = emptyList(), fogParticles = emptyList()) }
                    if (worldSize.isPlayable()) resetWorld()
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
                if (_state.value.phase != GamePhase.Running || !worldSize.isPlayable()) return
                val d = dragon ?: return
                val before = d.headPosition
                d.moveBy(intent.dx, intent.dy, worldSize)
                balance.moved((d.headPosition - before).getDistance())
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
        dragon = Dragon(Offset(centerX, centerY), initialLength = 1, initialLight = LightBalance.INITIAL_LIGHT)
        balance = LightBalance()
        checkpointMs = 0L
        world = World(worldSize, initialSoulCount = 5)
        frameCount = 0
        lastFpsUpdate = System.nanoTime() / 1_000_000L
        _state.update { it.copy(score = 0) }
        publishWorld(0)
        checkpoint()
    }

    private fun startInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        if (world != null) loop.start()
        audioManager.prepareMusic("ambient")
        audioManager.startMusic()
    }

    private fun pauseInternal() {
        loop.stop()
        _state.update { it.copy(phase = GamePhase.Paused) }
        audioManager.pauseMusic()
        audioManager.playSfx(Sfx.Pause)
        checkpoint()
    }

    private fun resumeInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        frameCount = 0
        lastFpsUpdate = System.nanoTime() / 1_000_000L
        if (world != null) loop.start()
        audioManager.playSfx(Sfx.Resume)
        audioManager.startMusic()
    }

    private fun stopInternal(gameOver: Boolean) {
        loop.stop()
        val score = _state.value.score
        viewModelScope.launch {
            gameRepository.updateHighScore(score)
        }
        if (gameOver) save(null) else checkpoint()
        if (gameOver) {
            audioManager.playSfx(Sfx.GameOver)
        }
        audioManager.pauseMusic()
        _state.update { it.copy(phase = if (gameOver) GamePhase.GameOver else GamePhase.Idle) }
    }

    private fun checkpoint() {
        val d = dragon ?: return
        val w = world ?: return
        if (d.light <= 0f) return
        fun point(p: Offset) = SavedPoint(p.x, p.y)
        save(SavedGame(
            width = worldSize.width, height = worldSize.height,
            segments = d.getSegments().map { point(it.position) },
            light = d.light, score = _state.value.score,
            souls = w.getSouls().map { SavedSoul(it.id, point(it.position), it.radius, it.lightIntensity, it.pulsePhase) },
            fog = w.getFogParticles().map { SavedFog(point(it.position), it.size, it.opacity, it.speed, it.direction) },
            worldTimeMs = w.getTimeMs(), idleMs = balance.idleMs, erosionMs = balance.erosionMs
        ))
        checkpointMs = 0
    }

    private fun save(game: SavedGame?) {
        savedGame = game
        _state.update { it.copy(hasSavedGame = game != null) }
        val revision = ++saveRevision
        gameRepository.saveGame(game) { error ->
            // Publish on Main, and do not let an older write overwrite a newer result.
            viewModelScope.launch {
                if (revision == saveRevision) _state.update { it.copy(
                    persistenceError = if (error == null) null else "Sauvegarde impossible. La dernière sauvegarde réussie reste disponible."
                ) }
            }
        }
    }

    private fun restoreGame(saved: SavedGame) {
        loop.stop()
        worldSize = Size(saved.width, saved.height)
        fun offset(p: SavedPoint) = Offset(p.x, p.y)
        dragon = Dragon().apply { restore(saved.segments.map(::offset), saved.light) }
        world = World(worldSize, initialSoulCount = 0).apply {
            restore(
                saved.souls.map { Soul(it.id, offset(it.position), it.radius, it.light, it.pulse) },
                saved.fog.map { FogParticle(offset(it.position), it.size, it.opacity, it.speed, it.direction) },
                saved.worldTimeMs
            )
        }
        if (viewportSize.isPlayable() && viewportSize != worldSize) {
            dragon?.resize(worldSize, viewportSize)
            world?.updateSize(viewportSize)
            worldSize = viewportSize
        }
        balance = LightBalance(saved.idleMs, saved.erosionMs)
        checkpointMs = 0
        _state.update { it.copy(phase = GamePhase.Paused, score = saved.score) }
        audioManager.prepareMusic("ambient")
        publishWorld(0)
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { gameRepository.setMusicEnabled(enabled) }
        audioManager.setMusicEnabled(enabled)
    }

    fun setSfxEnabled(enabled: Boolean) {
        viewModelScope.launch { gameRepository.setSfxEnabled(enabled) }
        audioManager.setSfxEnabled(enabled)
    }

    override fun onCleared() {
        loop.stop()
        audioManager.release()
        super.onCleared()
    }
}
