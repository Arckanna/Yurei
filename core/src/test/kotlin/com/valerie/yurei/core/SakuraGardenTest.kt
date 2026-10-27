package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.*
import org.junit.Assert.*
import org.junit.Test

class SakuraGardenTest {
    @Test fun stoneContactMatchesVisibleEllipseInEveryDirection() {
        val size = GameViewport.worldSize
        val stone = SakuraGarden.stones(size).first()
        val a = Dragon.HEAD_HALF_WIDTH
        val b = Dragon.HEAD_HALF_HEIGHT
        // Construct tangencies from ellipse boundary points and their outward normals.
        repeat(16) { i ->
            val angle = i * Math.PI / 8
            val boundary = Offset(a * kotlin.math.cos(angle).toFloat(), b * kotlin.math.sin(angle).toFloat())
            val normal = Offset(boundary.x / (a * a), boundary.y / (b * b))
            val unit = normal / normal.getDistance()
            val tangent = boundary + unit * stone.radius
            assertFalse("Separated at angle $angle", SakuraGarden.touchesStone(stone.center + tangent + unit * 0.1f, size))
            assertTrue("Overlapping at angle $angle", SakuraGarden.touchesStone(stone.center + tangent - unit * 0.1f, size))
        }
    }

    @Test fun collectionReachDoesNotCauseStoneDamage() {
        val size = GameViewport.worldSize
        val stone = SakuraGarden.stones(size).first()
        val head = stone.center + Offset(0f, stone.radius + 24f)
        assertTrue((head - stone.center).getDistance() < stone.radius + Dragon.HEAD_RADIUS)
        assertFalse(SakuraGarden.touchesStone(head, size))
        val dragon = Dragon(head)
        for (light in listOf(0.15f, 1f)) {
            dragon.setLight(light)
            assertEquals(48f, dragon.getHeadRadius(), 0f)
        }
    }

    @Test fun stonesLeaveTheStartingPointSafeAndHaveConsistentCollisionBoundaries() {
        val size = GameViewport.worldSize
        assertFalse(SakuraGarden.touchesStone(Offset(540f,960f), size))
        SakuraGarden.stones(size).forEach { stone ->
            assertTrue(SakuraGarden.touchesStone(stone.center, size))
            assertFalse(SakuraGarden.touchesStone(stone.center + Offset(stone.radius + Dragon.HEAD_HALF_WIDTH + 1, 0f), size))
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
