package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.TimedChallenge
import org.junit.Assert.*
import org.junit.Test

class TimedChallengeTest {
    @Test fun deadlineAndLastSoulHaveUnambiguousOutcome() {
        val win = TimedChallenge(100, 29)
        win.tick(50, 1, false)
        assertTrue(win.won)
        win.tick(1000, 0, false)
        assertEquals(50L, win.remainingMs)
        val lose = TimedChallenge(50, 29)
        lose.tick(50, 1, false)
        assertTrue(lose.lost)
        assertFalse(lose.won)
    }

    @Test fun collisionCostsTenSecondsAndGraceSurvivesRestoration() {
        val run = TimedChallenge()
        assertTrue(run.tick(16, 0, true))
        assertEquals(169984L, run.remainingMs)
        val restored = TimedChallenge(run.remainingMs, run.collected, run.graceMs)
        assertFalse(restored.tick(1000, 0, true))
        assertTrue(restored.tick(1000, 0, true))
        assertEquals(157984L, restored.remainingMs)
    }

    @Test fun tailDetectionIncludesSpaceBetweenSegmentsButExcludesNeck() {
        val dragon = Dragon()
        dragon.restore(listOf(Offset(100f,100f), Offset(100f,110f), Offset(100f,120f),
            Offset(200f,150f), Offset(200f,50f), Offset(50f,100f), Offset(150f,100f)), 1f)
        assertTrue(TimedChallenge().touchesTail(dragon))
        dragon.restore(listOf(Offset(100f,100f), Offset(100f,110f)), 1f)
        assertFalse(TimedChallenge().touchesTail(dragon))
    }

    @Test fun steeringCannotTeleportAndMotionContinuesWithoutInput() {
        val dragon = Dragon(Offset(150f,250f))
        val run = TimedChallenge()
        run.steer(10000f, 0f)
        assertEquals(Offset(150f,250f), dragon.headPosition)
        run.move(dragon, Size(300f,500f), 50)
        assertEquals(3.3f, (dragon.headPosition - Offset(150f,250f)).getDistance(), 0.001f)
        repeat(1000) { run.move(dragon, Size(300f,500f), 16) }
        assertTrue(dragon.headPosition.x in 48f..252f)
        assertTrue(dragon.headPosition.y in 48f..452f)
    }
}
