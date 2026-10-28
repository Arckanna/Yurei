package com.valerie.yurei

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.valerie.yurei.data.model.*
import com.valerie.yurei.data.repository.GameRepository
import com.valerie.yurei.ui.viewmodel.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MoonLevelTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun fixture(level: Int = 3, completed: Boolean = false) = SavedGame(
        version = 7, level = level, width = 1080f, height = 1920f,
        segments = listOf(SavedPoint(540f, 960f)), deployedSegments = listOf(false),
        light = .8f, score = if (completed) 300 else 0, souls = emptyList(), fog = emptyList(),
        worldTimeMs = 0, idleMs = 0, erosionMs = 0, remainingMs = 180000,
        collected = if (completed) 30 else 0, completed = completed, chapter = if (completed) 2 else 0)

    private fun ready(scenario: ActivityScenario<MainActivity>): GameViewModel {
        lateinit var vm: GameViewModel
        scenario.onActivity { vm = ViewModelProvider(it)[GameViewModel::class.java] }
        val deadline = SystemClock.elapsedRealtime() + 5000
        while (!vm.state.value.ready && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(20)
        assertTrue(vm.state.value.ready)
        instrumentation.waitForIdleSync()
        return vm
    }

    @Test fun secondMemoryUnlocksMoonAndPausedTideRestores() = runBlocking {
        val repository = GameRepository(instrumentation.targetContext)
        repository.saveGame(fixture(2, completed = true)); repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = ready(scenario)
            scenario.onActivity { vm.event(GameIntent.StartLevel(3)) }
            assertEquals(3, vm.state.value.level)
            assertEquals(GamePhase.Intro, vm.state.value.phase)
            SystemClock.sleep(700); screenshot("moon-intro.png")
            scenario.onActivity { vm.event(GameIntent.Begin) }
            SystemClock.sleep(2000); screenshot("moon-playing.png")
            scenario.onActivity { vm.pause() }
            val time = vm.state.value.worldTimeMs
            SystemClock.sleep(300)
            assertEquals(time, vm.state.value.worldTimeMs)
            assertEquals(time, repository.loadGame()!!.worldTimeMs)
        }
        val saved = repository.loadGame()!!
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = ready(scenario)
            assertEquals(3, vm.state.value.level)
            assertEquals(saved.worldTimeMs, vm.state.value.worldTimeMs)
            assertEquals(GamePhase.Paused, vm.state.value.phase)
        }
    }

    @Test fun activeSealPenaltyAndFinalMemoryArePersistent() = runBlocking {
        val repository = GameRepository(instrumentation.targetContext)
        repository.saveGame(fixture().copy(worldTimeMs = 7000,
            segments = listOf(SavedPoint(324f, 729.6f))))
        repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = ready(scenario)
            scenario.onActivity { vm.event(GameIntent.Continue); vm.resume() }
            SystemClock.sleep(300)
            scenario.onActivity { vm.pause() }
            assertEquals(true, vm.state.value.hitScenery)
            // A busy emulator may spend the whole protection interval waiting for UI idle.
            assertEquals(vm.state.value.collisionGraceMs, repository.loadGame()!!.graceMs)
            assertTrue(vm.state.value.remainingMs < 170000)
            assertEquals(true, repository.loadGame()!!.hitScenery)
        }
        repository.saveGame(fixture().copy(collected = 29, score = 290,
            souls = listOf(SavedSoul(1, SavedPoint(540f, 960f), 20f, 1f, 0f))))
        repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = ready(scenario)
            scenario.onActivity { vm.event(GameIntent.Continue); vm.resume() }
            SystemClock.sleep(700)
            assertEquals(GamePhase.Completed, vm.state.value.phase)
            assertTrue(vm.state.value.moonMemoryUnlocked)
            assertTrue(repository.loadGame()!!.completed)
            assertTrue(repository.preferences.first().moonMemoryUnlocked)
            screenshot("moon-memory.png")
            scenario.onActivity { vm.quit(); vm.event(GameIntent.Continue) }
            assertEquals(GamePhase.Completed, vm.state.value.phase)
            assertEquals(3, vm.state.value.level)
        }
    }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
