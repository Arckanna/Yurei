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
    @Test fun pendingTimeCanBeSettledBeforePauseExactlyOnce() = runTest {
        var now = 100L
        val loop = GameLoop(backgroundScope, clockMs = { now }) { _, _ -> fail("No frame expected") }
        loop.start()
        runCurrent()
        now += 1234
        assertEquals(1234L, loop.consumePendingTime())
        assertEquals(0L, loop.consumePendingTime())
        loop.stop()
        now += 60_000
        assertEquals(0L, loop.consumePendingTime())
    }

    @Test fun realDeadlineExpiresDespiteCappedSimulationAndPauseIsExcluded() = runTest {
        var now = 0L
        var simulated = 0L
        val challenge = com.valerie.yurei.core.world.TimedChallenge()
        val loop = GameLoop(backgroundScope, clockMs = { now }) { simulationMs, activeMs ->
            simulated += simulationMs
            challenge.tick(activeMs, 0, false)
        }
        loop.start()
        runCurrent()
        now = 90_000
        advanceTimeBy(17)
        assertEquals(90_000L, challenge.remainingMs)
        assertEquals(50L, simulated)
        loop.stop()
        now += 600_000 // Ten minutes paused must not consume the remaining time.
        loop.start()
        runCurrent()
        now += 89_999
        advanceTimeBy(16)
        runCurrent()
        assertEquals(1L, challenge.remainingMs)
        assertFalse(challenge.lost)
        now += 1
        advanceTimeBy(16)
        runCurrent()
        assertTrue(challenge.lost)
        assertEquals(101L, simulated)
        loop.stop()
    }

    @Test fun slowFramesConsumeTheirEntireDurationAndCollisionGrace() = runTest {
        var now = 0L
        val challenge = com.valerie.yurei.core.world.TimedChallenge(graceMs = 2000)
        val frames = mutableListOf<Pair<Long, Long>>()
        val loop = GameLoop(backgroundScope, clockMs = { now }) { simulationMs, activeMs ->
            frames.add(simulationMs to activeMs)
            challenge.tick(activeMs, 0, false)
        }
        loop.start()
        runCurrent()
        repeat(20) {
            now += 100
            advanceTimeBy(16)
            runCurrent()
        }
        assertEquals(178_000L, challenge.remainingMs)
        assertEquals(0L, challenge.graceMs)
        assertEquals(List(20) { 50L to 100L }, frames)
        loop.stop()
    }

    @Test fun repeatedStartAndImmediateRestartKeepOnlyOneLoop() = runTest {
        val updates = mutableListOf<Long>()
        val loop = GameLoop(backgroundScope, clockMs = { testScheduler.currentTime }) { simulationMs, _ -> updates.add(simulationMs) }
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
        val loop = GameLoop(backgroundScope, clockMs = { testScheduler.currentTime }) { simulationMs, _ -> updates.add(simulationMs) }
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
        val loop = GameLoop(backgroundScope, clockMs = { now }) { simulationMs, _ -> updates.add(simulationMs) }
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
        loop = GameLoop(backgroundScope, clockMs = { testScheduler.currentTime }) { _, _ ->
            count++
            loop.stop()
        }
        loop.start()
        runCurrent()
        advanceTimeBy(1000)
        assertEquals(1, count)
    }
}
