package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.*
import org.junit.Assert.*
import org.junit.Test

class SakuraGardenTest {
    @Test fun stonesLeaveTheStartingPointSafeAndHaveConsistentCollisionBoundaries() {
        val size = GameViewport.worldSize
        assertFalse(SakuraGarden.touchesStone(Offset(540f,960f), Dragon.HEAD_RADIUS, size))
        SakuraGarden.stones(size).forEach { stone ->
            assertTrue(SakuraGarden.touchesStone(stone.center, Dragon.HEAD_RADIUS, size))
            assertFalse(SakuraGarden.touchesStone(stone.center + Offset(stone.radius + Dragon.HEAD_RADIUS + 1, 0f), Dragon.HEAD_RADIUS, size))
        }
    }

    @Test fun initialAndReplenishedSoulsAlwaysLeaveRoomAroundStones() {
        val world = World(GameViewport.worldSize, level = 2)
        repeat(100) {
            val souls = world.getSouls()
            assertTrue(souls.isNotEmpty())
            assertTrue(souls.all { SakuraGarden.isSafeForSoul(it.position, GameViewport.worldSize) })
            souls.forEach { world.checkSoulCollisions(it.position, 48f) }
            world.update(16)
        }
    }

    @Test fun obstacleUsesSharedPenaltyAndGraceInsteadOfStackingDamage() {
        val run = TimedChallenge()
        assertTrue(run.tick(16, 0, true))
        assertEquals(169984L, run.remainingMs)
        assertFalse(run.tick(16, 0, true))
        assertEquals(169968L, run.remainingMs)
    }
}
