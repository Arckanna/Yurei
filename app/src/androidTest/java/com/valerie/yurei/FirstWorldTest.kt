package com.valerie.yurei

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import com.valerie.yurei.data.repository.GameRepository
import com.valerie.yurei.data.model.SavedGame
import com.valerie.yurei.data.model.SavedPoint
import com.valerie.yurei.ui.viewmodel.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Real-time traversal: no test clock, no accelerated production rules. */
class FirstWorldTest {
    @Test fun expiredRunCannotBeContinuedAfterQuitting() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val repository = GameRepository(instrumentation.targetContext)
        repository.saveGame(SavedGame(version = 4,
            width = 300f, height = 500f, segments = listOf(SavedPoint(150f, 250f)),
            light = 0.15f, score = 190, souls = emptyList(), fog = emptyList(),
            worldTimeMs = 179900, idleMs = 0, erosionMs = 0,
            remainingMs = 100, collected = 29, journeyStarted = true))
        repository.loadGame()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: GameViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[GameViewModel::class.java] }
            val deadline = SystemClock.elapsedRealtime() + 5000
            while (!vm.state.value.ready && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(20)
            assertTrue(vm.state.value.ready)
            instrumentation.waitForIdleSync()
            SystemClock.sleep(800)
            scenario.onActivity { vm.event(GameIntent.Continue); vm.resume() }
            val end = SystemClock.elapsedRealtime() + 5000
            while (vm.state.value.phase == GamePhase.Running && SystemClock.elapsedRealtime() < end) SystemClock.sleep(20)
            assertEquals(GamePhase.GameOver, vm.state.value.phase)
            assertEquals(0L, vm.state.value.remainingMs)
            SystemClock.sleep(500)
            screenshot("world-timeout.png")
            scenario.onActivity { vm.quit() }
            assertNull(repository.loadGame())
        }
    }

    @Test fun completedSaveReopensMemory() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val repository = GameRepository(instrumentation.targetContext)
        repository.saveGame(SavedGame(
            width = 300f, height = 500f, segments = listOf(SavedPoint(150f, 250f)),
            light = 0.8f, score = 120, souls = emptyList(), fog = emptyList(),
            worldTimeMs = 180000, idleMs = 0, erosionMs = 0,
            chapter = 2, chapterMs = 60000, offerings = 4, journeyMs = 180000,
            completed = true, journeyStarted = true
        ))
        assertTrue(repository.loadGame()!!.completed)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: GameViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[GameViewModel::class.java] }
            val deadline = SystemClock.elapsedRealtime() + 5000
            while (!vm.state.value.ready && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(20)
            assertTrue(vm.state.value.ready)
            // Let the first composition attach its navigator before dispatching Continue.
            instrumentation.waitForIdleSync()
            SystemClock.sleep(800)
            scenario.onActivity { vm.event(GameIntent.Continue) }
            assertEquals(GamePhase.Completed, vm.state.value.phase)
            SystemClock.sleep(1000)
            screenshot("world-memory.png")
        }
    }

    @Test fun thirtySoulsReachMemoryBeforeDeadline() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val repository = GameRepository(context)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: GameViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[GameViewModel::class.java] }
            val loadDeadline = SystemClock.elapsedRealtime() + 5000
            while (!vm.state.value.ready && SystemClock.elapsedRealtime() < loadDeadline) SystemClock.sleep(20)
            assertTrue(vm.state.value.ready)
            scenario.onActivity { vm.start() }
            SystemClock.sleep(600)
            screenshot("world-intro.png")
            assertEquals(GamePhase.Intro, vm.state.value.phase)
            scenario.onActivity { vm.event(GameIntent.Begin) }
            val started = SystemClock.elapsedRealtime()
            var lastMove = 0L
            var captured = false
            while (vm.state.value.phase == GamePhase.Running && SystemClock.elapsedRealtime() - started < 320_000) {
                val state = vm.state.value
                if (state.journeyMs - lastMove >= 100) {
                    scenario.onActivity {
                        val current = vm.state.value
                        val head = current.dragon.segments.firstOrNull()?.position
                        val soul = current.souls.minByOrNull { (it.position - (head ?: it.position)).getDistance() }?.position
                        if (head != null && soul != null) vm.onDrag(soul.x - head.x, soul.y - head.y)
                    }
                    lastMove = state.journeyMs
                }
                if (!captured && state.journeyMs >= 12_000) {
                    screenshot("world-playing.png")
                    captured = true
                }
                SystemClock.sleep(100)
            }
            val ending = vm.state.value
            assertEquals(GamePhase.Completed, ending.phase)
            assertEquals(30, ending.collected)
            assertTrue(ending.remainingMs > 0)
            assertTrue(ending.memoryUnlocked)
            assertNull(ending.persistenceError)
            SystemClock.sleep(600)
            screenshot("world-memory.png")
            val saved = repository.loadGame()!!
            assertTrue(saved.completed)
            assertEquals(2, saved.chapter)
            // Leaving and reopening the completed save must retain the memory.
            scenario.onActivity { vm.quit(); vm.event(GameIntent.Continue) }
            assertEquals(GamePhase.Completed, vm.state.value.phase)
            assertTrue(repository.loadGame()!!.completed)
        }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
