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

class SakuraLevelTest {
    @Test fun completeGardenRunReachesThirtySouls() = runBlocking {
        val repository = GameRepository(instrumentation.targetContext)
        repository.saveGame(fixture(1, completed = true))
        repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = ready(scenario)
            scenario.onActivity { vm.event(GameIntent.StartLevel(2)); vm.event(GameIntent.Begin) }
            val deadline = SystemClock.elapsedRealtime() + 185000
            while (vm.state.value.phase == GamePhase.Running && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity {
                    val state = vm.state.value
                    val head = state.dragon.segments.firstOrNull()?.position
                    if (head != null) {
                        val soul = state.souls.minByOrNull { (it.position - head).getDistance() }
                        if (soul != null) vm.onDrag(soul.position.x - head.x, soul.position.y - head.y)
                    }
                }
                SystemClock.sleep(100)
            }
            assertEquals(GamePhase.Completed, vm.state.value.phase)
            assertEquals(30, vm.state.value.collected)
            assertTrue(vm.state.value.remainingMs > 0)
            assertTrue(repository.loadGame()!!.completed)
        }
    }

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun fixture(level: Int, completed: Boolean = false) = SavedGame(
        version = 5, level = level, width = 1080f, height = 1920f,
        segments = listOf(SavedPoint(540f,960f)), light = 0.8f, score = if (completed) 300 else 0,
        souls = emptyList(), fog = emptyList(), worldTimeMs = 1000, idleMs = 0, erosionMs = 0,
        collected = if (completed) 30 else 0, remainingMs = 120000, completed = completed,
        chapter = if (completed) 2 else 0, journeyStarted = true)

    private fun ready(scenario: ActivityScenario<MainActivity>): GameViewModel {
        lateinit var vm: GameViewModel
        scenario.onActivity { vm = ViewModelProvider(it)[GameViewModel::class.java] }
        val deadline = SystemClock.elapsedRealtime() + 5000
        while (!vm.state.value.ready && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(20)
        assertTrue(vm.state.value.ready)
        instrumentation.waitForIdleSync()
        SystemClock.sleep(700)
        return vm
    }

    @Test fun firstMemoryUnlocksSakuraAndRecreationKeepsSecondLevel() = runBlocking {
        val repository = GameRepository(instrumentation.targetContext)
        repository.saveGame(fixture(1, completed = true))
        repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var vm = ready(scenario)
            scenario.onActivity { vm.event(GameIntent.Continue); vm.event(GameIntent.StartLevel(2)) }
            assertEquals(GamePhase.Intro, vm.state.value.phase)
            assertEquals(2, vm.state.value.level)
            SystemClock.sleep(700)
            screenshot("sakura-intro.png")
            scenario.onActivity { vm.event(GameIntent.Begin) }
            SystemClock.sleep(1500)
            screenshot("sakura-playing.png")
            scenario.onActivity { vm.pause() }
            val remaining = vm.state.value.remainingMs
            assertEquals(2, repository.loadGame()!!.level)
            scenario.recreate()
            vm = ready(scenario)
            assertEquals(2, vm.state.value.level)
            assertEquals(GamePhase.Paused, vm.state.value.phase)
            assertEquals(remaining, vm.state.value.remainingMs)
            scenario.onActivity { vm.quit(); vm.event(GameIntent.StartLevel(1)) }
            assertEquals(1, vm.state.value.level)
        }
    }

    @Test fun stonePenaltyAndFinalMemorySurviveSaving() = runBlocking {
        val repository = GameRepository(instrumentation.targetContext)
        repository.saveGame(fixture(2).copy(segments = listOf(SavedPoint(324f,729.6f))))
        repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = ready(scenario)
            scenario.onActivity { vm.event(GameIntent.Continue); vm.resume() }
            SystemClock.sleep(400)
            scenario.onActivity { vm.pause() }
            assertEquals(true, vm.state.value.hitScenery)
            assertTrue(vm.state.value.collisionGraceMs > 0)
            assertTrue(vm.state.value.remainingMs in 108000..110000)
            assertEquals(2, repository.loadGame()!!.level)
        }
        repository.saveGame(fixture(2).copy(collected = 29, score = 290,
            souls = listOf(SavedSoul(1, SavedPoint(540f,960f),20f,1f,0f))))
        repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = ready(scenario)
            scenario.onActivity { vm.event(GameIntent.Continue); vm.resume() }
            SystemClock.sleep(700)
            assertEquals(GamePhase.Completed, vm.state.value.phase)
            assertTrue(vm.state.value.sakuraMemoryUnlocked)
            assertTrue(repository.loadGame()!!.completed)
            assertTrue(repository.preferences.first().sakuraMemoryUnlocked)
            screenshot("sakura-memory.png")
            scenario.onActivity { vm.quit(); vm.event(GameIntent.Continue) }
            assertEquals(GamePhase.Completed, vm.state.value.phase)
            assertEquals(2, vm.state.value.level)
        }
    }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
        bitmap.recycle()
    }
}
