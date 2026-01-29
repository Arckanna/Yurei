package com.valerie.yurei.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valerie.yurei.core.engine.GameLoop
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.World
import com.valerie.yurei.ui.navigation.RootNav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GameViewModel(
    private val nav: RootNav
) : ViewModel() {

    private var dragon: Dragon? = null
    private var world: World? = null
    private var worldSize: Size = Size(1080f, 1920f) // Taille par défaut (sera mise à jour)

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    // Compteur de FPS
    private var frameCount = 0
    private var lastFpsUpdate = System.currentTimeMillis()

    private val loop = GameLoop(viewModelScope) { dt ->
        val d = dragon ?: return@GameLoop
        val w = world ?: return@GameLoop

        // Mettre à jour le monde
        w.update(dt)

        // Diminuer la lumière progressivement (perte si immobile)
        val currentLight = d.light
        val lightDecay = 0.0001f * dt // Perte de lumière par milliseconde
        d.setLight((currentLight - lightDecay).coerceAtLeast(0f))

        // Vérifier les collisions avec les âmes
        val collectedSoulIds = w.checkSoulCollisions(d.headPosition, d.getHeadRadius())
        if (collectedSoulIds.isNotEmpty()) {
            // Ajouter un segment pour chaque âme collectée
            collectedSoulIds.forEach { _ ->
                d.addSegment()
                // Augmenter la lumière lors de la collecte
                d.setLight((d.light + 0.2f).coerceIn(0f, 1f))
            }
            // Mettre à jour le score
            _state.update { it.copy(score = it.score + collectedSoulIds.size * 10) }
        }

        // Mettre à jour l'état UI
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

        // Calculer le FPS
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

        // Game Over si la lumière est épuisée
        if (d.light <= 0f && _state.value.phase == GamePhase.Running) {
            stopInternal(gameOver = true)
            nav.toHome()
        }
    }

    /**
     * Met à jour la taille du monde (appelé depuis GameScreen).
     */
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
        _state.value = GameUiState(phase = GamePhase.Idle)
    }

    private fun startInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        loop.start()
    }

    private fun pauseInternal() {
        loop.stop()
        _state.update { it.copy(phase = GamePhase.Paused) }
    }

    private fun resumeInternal() {
        _state.update { it.copy(phase = GamePhase.Running) }
        loop.start()
    }

    private fun stopInternal(gameOver: Boolean) {
        loop.stop()
        _state.update { it.copy(phase = if (gameOver) GamePhase.GameOver else GamePhase.Idle) }
    }

    override fun onCleared() {
        loop.stop()
        super.onCleared()
    }
}
