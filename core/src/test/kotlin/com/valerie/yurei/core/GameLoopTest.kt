package com.valerie.yurei.core

import com.valerie.yurei.core.engine.GameLoop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameLoopTest {
    @Test fun repeatedStartAndImmediateRestartKeepOnlyOneLoop() = runTest {
        val updates = mutableListOf<Long>()
        val loop = GameLoop(backgroundScope, clockMs = { testScheduler.currentTime }) { updates.add(it) }
        loop.start()
        runCurrent()
        repeat(20) {
            loop.start()
            loop.stop()
            loop.start()
            runCurrent()
        }
        advanceTimeBy(161)
        assertEquals(10, updates.size)
        assertTrue(updates.all { it == 16L })
        loop.stop()
        advanceTimeBy(1000)
        assertEquals(10, updates.size)
    }

    @Test fun resumeDoesNotIncludePausedTime() = runTest {
        val updates = mutableListOf<Long>()
        val loop = GameLoop(backgroundScope, clockMs = { testScheduler.currentTime }) { updates.add(it) }
        loop.start()
        runCurrent()
        advanceTimeBy(17)
        loop.stop()
        advanceTimeBy(60_000)
        loop.start()
        runCurrent()
        advanceTimeBy(17)
        assertEquals(listOf(16L, 16L), updates)
        loop.stop()
    }

    @Test fun stallsAreCappedAndBackwardClockDoesNotProduceNegativeDelta() = runTest {
        var now = 100L
        val updates = mutableListOf<Long>()
        val loop = GameLoop(backgroundScope, clockMs = { now }) { updates.add(it) }
        loop.start()
        runCurrent()
        now = 20_000L
        advanceTimeBy(17)
        now = 10L
        advanceTimeBy(16)
        assertEquals(listOf(50L), updates)
        loop.stop()
    }

    @Test fun stopInsideUpdatePreventsFurtherTicks() = runTest {
        var count = 0
        lateinit var loop: GameLoop
        loop = GameLoop(backgroundScope, clockMs = { testScheduler.currentTime }) {
            count++
            loop.stop()
        }
        loop.start()
        runCurrent()
        advanceTimeBy(1000)
        assertEquals(1, count)
    }
}
