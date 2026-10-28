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
import com.valerie.yurei.core.world.GameViewport
import com.valerie.yurei.core.world.LanternJourney
import com.valerie.yurei.core.world.TimedChallenge
import com.valerie.yurei.core.world.SakuraGarden
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
    private var viewportSize: Size = GameViewport.worldSize

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    private var level = 1
    private var challenge = TimedChallenge()
    private var journey = LanternJourney()
    private var journeyStarted = false
    private var balance = LightBalance()
    private var checkpointMs = 0L
    private var savedGame: SavedGame? = null
    private var saveRevision = 0L

    private var frameCount = 0
    private var lastFpsUpdate = System.nanoTime() / 1_000_000L

    private val loop = GameLoop(viewModelScope) { dt, activeMs ->
        if (_state.value.phase != GamePhase.Running) return@GameLoop
        val d = dragon ?: return@GameLoop
        val w = world ?: return@GameLoop

        if (activeMs >= challenge.remainingMs) {
            challenge.tick(activeMs, 0, false)
            d.setLight(0.15f)
            publishWorld()
            stopInternal(gameOver = true)
            return@GameLoop
        }
        challenge.move(d, worldSize, dt)
        d.update(dt)
        w.update(dt)

        val collectedSoulIds = w.checkSoulCollisions(d.headPosition, d.getHeadRadius())
        if (collectedSoulIds.isNotEmpty()) {
            collectedSoulIds.forEach { _ ->
                d.addSegment()
            }
            _state.update { it.copy(score = it.score + collectedSoulIds.size * 10) }
            audioManager.playSfx(Sfx.SoulCollect)
        }

        val stoneHit = level == 2 && SakuraGarden.touchesStone(d.headPosition, worldSize)
        val hit = challenge.tick(activeMs, collectedSoulIds.size, challenge.touchesTail(d) || stoneHit)
        if (hit) {
            _state.update { it.copy(hitScenery = stoneHit) }
            repeat(3) { d.removeSegment() }
            audioManager.playSfx(Sfx.GameOver)
        }
        journey.elapsedMs += dt
        val chapterChanged = journey.chapter != (challenge.collected / 10).coerceAtMost(2)
        journey.chapter = (challenge.collected / 10).coerceAtMost(2)
        journey.chapterMs = if (chapterChanged) 0 else (journey.chapterMs + dt).coerceAtMost(100_000)
        journey.offerings = (challenge.collected % 10).coerceAtMost(4)
        journey.completed = challenge.won
        d.setLight((0.15f + 0.85f * challenge.remainingMs / 180_000f).coerceIn(0.15f, 1f))
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

        if (journey.completed) {
            finishJourney()
            return@GameLoop
        }
        checkpointMs += activeMs
        if (d.light > 0f && (checkpointMs >= 2_000L || collectedSoulIds.isNotEmpty() || chapterChanged || hit)) checkpoint()
        if (challenge.lost && _state.value.phase == GamePhase.Running) {
            stopInternal(gameOver = true)
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
                            memoryUnlocked = prefs.memoryUnlocked || it.memoryUnlocked,
                            sakuraMemoryUnlocked = prefs.sakuraMemoryUnlocked || it.sakuraMemoryUnlocked,
                            musicEnabled = prefs.musicEnabled,
                            sfxEnabled = prefs.sfxEnabled
                        )
                    }
                    audioManager.setMusicEnabled(prefs.musicEnabled)
                    audioManager.setSfxEnabled(prefs.sfxEnabled)
                }
        }
    }

    private fun finishJourney() {
        loop.stop()
        _state.update { it.copy(phase = GamePhase.Completed, memoryUnlocked = true, sakuraMemoryUnlocked = it.sakuraMemoryUnlocked || level == 2) }
        checkpoint()
        audioManager.prepareMusic(if (level == 2) "sakura_memory" else "memory", loop = false)
        audioManager.startMusic()
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
            level = level,
            chapter = journey.chapter,
            chapterMs = journey.chapterMs,
            offerings = journey.offerings,
            journeyProgress = challenge.collected / 30f,
            remainingMs = challenge.remainingMs,
            collected = challenge.collected,
            collisionGraceMs = challenge.graceMs,
            journeyMs = journey.elapsedMs,
            refuge = journey.refuge(worldSize),
            refugeRadius = journey.refugeRadius(worldSize),
            sheltered = false,
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
        val logicalSize = GameViewport.worldSize
        viewportSize = logicalSize
        val previousSize = worldSize
        worldSize = logicalSize
        if (world == null) {
            if (_state.value.phase == GamePhase.Running || _state.value.phase == GamePhase.Paused || _state.value.phase == GamePhase.Intro) {
                resetWorld()
                if (_state.value.phase == GamePhase.Running) loop.start()
            }
        } else if (logicalSize != previousSize) {
            dragon?.resize(previousSize, logicalSize)
            world?.updateSize(logicalSize)
            publishWorld()
        }
    }

    fun event(intent: GameIntent) {
        if (!_state.value.ready) return
        when (intent) {
            is GameIntent.Begin -> {
                if (_state.value.phase == GamePhase.Intro) {
                    journeyStarted = true
                    startInternal()
                    checkpoint()
                }
            }
            is GameIntent.Continue -> {
                val saved = savedGame ?: return
                restoreGame(saved)
                nav.toGame()
                if (saved.completed) audioManager.startMusic(explicitResume = true)
            }
            is GameIntent.Start -> startLevel(level)
            is GameIntent.StartLevel -> startLevel(intent.level)
            is GameIntent.Pause -> {
                if (_state.value.phase == GamePhase.Running) pauseInternal()
                else {
                    audioManager.pauseMusic()
                    if (_state.value.phase == GamePhase.Intro || _state.value.phase == GamePhase.Completed) checkpoint()
                }
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
                if (dragon == null) return
                challenge.steer(intent.dx, intent.dy)
            }
        }
    }

    private fun startLevel(requested: Int) {
        if (requested !in 1..2 || _state.value.phase == GamePhase.Running) return
        if (requested == 2 && !_state.value.memoryUnlocked) return
        save(null)
        loop.stop()
        audioManager.pauseMusic()
        level = requested
        dragon = null
        world = null
        _state.update { it.copy(level = level, phase = GamePhase.Intro, score = 0, hitScenery = false,
            dragon = DragonVM(), souls = emptyList(), fogParticles = emptyList()) }
        if (worldSize.isPlayable()) resetWorld()
        nav.toGame()
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
        challenge = TimedChallenge()
        journey = LanternJourney()
        journeyStarted = _state.value.phase == GamePhase.Running
        checkpointMs = 0L
        world = World(worldSize, initialSoulCount = 5, level = level)
        frameCount = 0
        lastFpsUpdate = System.nanoTime() / 1_000_000L
        _state.update { it.copy(score = 0) }
        publishWorld(0)
        checkpoint()
    }

    private fun startInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        if (world != null) loop.start()
        audioManager.prepareMusic(if (level == 2) "sakura" else "ambient")
        audioManager.startMusic(explicitResume = true)
    }

    private fun settleActiveTime(): Boolean {
        challenge.tick(loop.consumePendingTime(), 0, false)
        dragon?.setLight((0.15f + 0.85f * challenge.remainingMs / 180_000f).coerceIn(0.15f, 1f))
        publishWorld()
        return challenge.lost
    }

    private fun pauseInternal() {
        if (settleActiveTime()) {
            stopInternal(gameOver = true)
            return
        }
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
        audioManager.startMusic(explicitResume = true)
    }

    private fun stopInternal(gameOver: Boolean) {
        val ended = gameOver || (_state.value.phase == GamePhase.Running && settleActiveTime())
        loop.stop()
        val score = _state.value.score
        viewModelScope.launch {
            gameRepository.updateHighScore(score)
        }
        if (ended) save(null) else checkpoint()
        if (ended) {
            audioManager.playSfx(Sfx.GameOver)
        }
        audioManager.pauseMusic()
        _state.update { it.copy(phase = if (ended) GamePhase.GameOver else GamePhase.Idle) }
    }

    private fun checkpoint() {
        val d = dragon ?: return
        val w = world ?: return
        if (d.light <= 0f || challenge.lost || _state.value.phase == GamePhase.GameOver) return
        fun point(p: Offset) = SavedPoint(p.x, p.y)
        save(SavedGame(
            version = 6, level = level, remainingMs = challenge.remainingMs, collected = challenge.collected,
            graceMs = challenge.graceMs, heading = challenge.heading, desiredHeading = challenge.desiredHeading,
            hitScenery = _state.value.hitScenery,
            width = worldSize.width, height = worldSize.height,
            segments = d.getSegments().map { point(it.position) },
            deployedSegments = d.getSegments().map { it.deployed },
            light = d.light, score = _state.value.score,
            souls = w.getSouls().map { SavedSoul(it.id, point(it.position), it.radius, it.lightIntensity, it.pulsePhase) },
            fog = w.getFogParticles().map { SavedFog(point(it.position), it.size, it.opacity, it.speed, it.direction) },
            worldTimeMs = w.getTimeMs(), idleMs = balance.idleMs, erosionMs = balance.erosionMs,
            chapter = journey.chapter, chapterMs = journey.chapterMs, offerings = journey.offerings,
            journeyMs = journey.elapsedMs, completed = journey.completed, journeyStarted = journeyStarted
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
        level = saved.level
        worldSize = Size(saved.width, saved.height)
        fun offset(p: SavedPoint) = Offset(p.x, p.y)
        dragon = Dragon().apply { restore(saved.segments.map(::offset), saved.light, saved.deployedSegments) }
        world = World(worldSize, initialSoulCount = 0, level = level).apply {
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
        challenge = if (saved.version >= 3) TimedChallenge(saved.remainingMs, if (saved.completed) 30 else saved.collected, saved.graceMs, saved.heading, saved.desiredHeading) else TimedChallenge(collected = if (saved.completed) 30 else 0)
        journey = LanternJourney(saved.chapter, saved.chapterMs, saved.offerings, saved.journeyMs, saved.completed)
        journeyStarted = saved.journeyStarted
        balance = LightBalance(saved.idleMs, saved.erosionMs)
        checkpointMs = 0
        _state.update { it.copy(phase = when {
            saved.completed -> GamePhase.Completed
            !saved.journeyStarted -> GamePhase.Intro
            else -> GamePhase.Paused
        }, score = saved.score, hitScenery = saved.hitScenery,
            memoryUnlocked = it.memoryUnlocked || saved.completed || level == 2,
            sakuraMemoryUnlocked = it.sakuraMemoryUnlocked || (saved.completed && level == 2)) }
        audioManager.prepareMusic(if (level == 2) { if (saved.completed) "sakura_memory" else "sakura" } else { if (saved.completed) "memory" else "ambient" }, loop = !saved.completed)
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
