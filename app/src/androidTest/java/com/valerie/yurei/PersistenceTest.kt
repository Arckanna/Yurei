package com.valerie.yurei

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.valerie.yurei.data.model.*
import com.valerie.yurei.data.repository.GameRepository
import com.valerie.yurei.ui.navigation.RootNav
import com.valerie.yurei.ui.viewmodel.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PersistenceTest {
    private val fixture = SavedGame(
        version = 5, remainingMs = 90000, collected = 8, graceMs = 750,
        width = 1080f, height = 1920f,
        segments = listOf(SavedPoint(150f, 250f), SavedPoint(140f, 240f)),
        light = 0.42f, score = 70,
        souls = listOf(SavedSoul(9, SavedPoint(80f, 80f), 20f, 0.8f, 1.3f)),
        fog = listOf(SavedFog(SavedPoint(20f, 50f), 60f, 0.2f, 0.3f, 0.4f)),
        worldTimeMs = 90000, idleMs = 1234, erosionMs = 456,
        chapter = 1, chapterMs = 30000, offerings = 2, journeyMs = 90000
    )

    // Run separately with -e persistenceMode seed / verify and force-stop between
    // the invocations to validate a real process boundary without reinstalling.
    @Test fun savedWorldRestoresInFreshViewModel() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val repository = GameRepository(app)
        val mode = InstrumentationRegistry.getArguments().getString("persistenceMode")
        if (mode != "verify") {
            repository.saveGame(fixture)
            assertEquals(fixture, repository.loadGame()) // FIFO barrier + disk read
        }
        if (mode == "seed") return@runBlocking
        assertEquals(fixture, repository.loadGame())
        val store = ViewModelStore()
        lateinit var vm: GameViewModel
        val nav = object : RootNav {
            override fun toHome() {}
            override fun toGame() {}
            override fun toSettings() {}
            override fun back() {}
        }
        instrumentation.runOnMainSync {
            vm = ViewModelProvider(store, GameViewModelFactory(app, nav))[GameViewModel::class.java]
        }
        try {
            val deadline = SystemClock.elapsedRealtime() + 5000
            while (!vm.state.value.ready && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(10)
            val restored = vm.state.value
            assertTrue(restored.ready)
            assertNull(restored.persistenceError)
            assertTrue(restored.hasSavedGame)
            assertEquals(GamePhase.Paused, restored.phase)
            assertEquals(70, restored.score)
            assertEquals(2, restored.dragon.segments.size)
            assertEquals(0.42f, restored.dragon.segments.first().light, 0.00001f)
            assertEquals(150f, restored.dragon.segments.first().position.x, 0f)
            SystemClock.sleep(200)
            // Preferences load independently; only the paused simulation must stay frozen.
            val paused = vm.state.value
            assertEquals(restored.dragon, paused.dragon)
            assertEquals(restored.souls, paused.souls)
            assertEquals(restored.fogParticles, paused.fogParticles)
            assertEquals(restored.journeyMs, paused.journeyMs)
            assertEquals(GamePhase.Paused, paused.phase)
            assertEquals(90000L, paused.remainingMs)
            assertEquals(8, paused.collected)
            assertEquals(750L, paused.collisionGraceMs)
            assertEquals(1, paused.chapter)
            assertEquals(90000L, paused.journeyMs)
            // Changing physical resolution must not reshape a paused logical world.
            listOf(androidx.compose.ui.geometry.Size(360f, 640f),
                androidx.compose.ui.geometry.Size(1600f, 900f)).forEach { surface ->
                instrumentation.runOnMainSync { vm.updateWorldSize(surface) }
                assertEquals(paused.dragon, vm.state.value.dragon)
                assertEquals(paused.souls, vm.state.value.souls)
                assertEquals(paused.remainingMs, vm.state.value.remainingMs)
            }
            // Quit must retain the world, and a new game must invalidate it.
            instrumentation.runOnMainSync { vm.quit() }
            assertEquals(fixture, repository.loadGame())
            instrumentation.runOnMainSync { vm.start(); vm.pause() }
            val fresh = repository.loadGame()!!
            assertEquals(0, fresh.score)
            assertEquals(1, fresh.segments.size)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            repository.saveGame(null)
            assertNull(repository.loadGame())
        }
    }
}
