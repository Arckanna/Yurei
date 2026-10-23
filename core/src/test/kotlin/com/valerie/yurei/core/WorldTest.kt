package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.world.World
import org.junit.Assert.*
import org.junit.Test

class WorldTest {
    @Test fun verySmallSurfaceDoesNotProduceInvalidSpawnRanges() {
        val world = World(Size(20f, 10f))
        assertTrue(world.getSouls().all { it.position == Offset(10f, 5f) })
        world.update(16)
    }

    @Test fun resizingPreservesSoulsAndTimeWhileMovingThemIntoBounds() {
        val world = World(Size(1080f, 1920f))
        world.update(16)
        val ids = world.getSouls().map { it.id }
        world.updateSize(Size(100f, 80f))
        assertEquals(ids, world.getSouls().map { it.id })
        assertEquals(16L, world.getTimeMs())
        assertTrue(world.getSouls().all { it.position.x in 30f..70f && it.position.y in 30f..50f })
        assertTrue(world.getFogParticles().all { it.position.x in 0f..100f && it.position.y in 0f..80f })
        world.updateSize(Size.Zero)
        assertEquals(ids, world.getSouls().map { it.id })
    }

    @Test fun collectedSoulCannotBeCollectedTwiceAndWorldReplenishes() {
        val world = World(Size(300f, 400f), initialSoulCount = 1)
        val soul = world.getSouls().single()
        assertEquals(listOf(soul.id), world.checkSoulCollisions(soul.position, 24f))
        assertTrue(world.checkSoulCollisions(soul.position, 24f).isEmpty())
        world.update(16)
        assertTrue(world.getSouls().size in 3..8)
        assertTrue(world.getSouls().none { it.id == soul.id })
    }
}
