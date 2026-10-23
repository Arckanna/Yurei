package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.entity.Dragon
import org.junit.Assert.*
import org.junit.Test

class DragonTest {
    @Test fun initialLengthAndGrowthStayConsistent() {
        val dragon = Dragon(Offset(100f, 100f), initialLength = 3)
        assertEquals(3, dragon.getSegments().size)
        dragon.addSegment()
        dragon.removeSegment()
        assertEquals(dragon.length, dragon.getSegments().size)
        repeat(10) { dragon.removeSegment() }
        assertEquals(1, dragon.length)
    }

    @Test fun movementStaysInsideBoundsAndRejectsInvalidInput() {
        val dragon = Dragon(Offset(100f, 100f))
        dragon.moveBy(10_000f, -10_000f, Size(300f, 500f))
        assertEquals(Offset(252f, 48f), dragon.headPosition)
        dragon.moveBy(Float.NaN, 1f, Size(300f, 500f))
        assertEquals(Offset(252f, 48f), dragon.headPosition)
        dragon.moveBy(100f, 100f, Size(20f, 10f))
        assertEquals(Offset(10f, 5f), dragon.headPosition)
    }

    @Test fun bodyFollowsOnTicksWithoutFurtherTouchEvents() {
        val dragon = Dragon(Offset.Zero, initialLength = 2)
        dragon.updateHeadPosition(Offset(300f, 0f))
        assertEquals(Offset.Zero, dragon.getSegments()[1].position)
        dragon.update(16)
        val firstX = dragon.getSegments()[1].position.x
        dragon.update(16)
        assertTrue(firstX > 0f)
        assertTrue(dragon.getSegments()[1].position.x > firstX)
    }

    @Test fun followingDependsOnElapsedTimeRatherThanInputFrequency() {
        val a = Dragon(Offset.Zero, initialLength = 2)
        val b = Dragon(Offset.Zero, initialLength = 2)
        a.updateHeadPosition(Offset(300f, 0f))
        repeat(10) { b.updateHeadPosition(Offset(300f, 0f)) }
        a.update(32)
        repeat(2) { b.update(16) }
        assertEquals(a.getSegments()[1].position.x, b.getSegments()[1].position.x, 0.001f)
    }

    @Test fun resizePreservesProgressAndKeepsAllSegmentsVisible() {
        val dragon = Dragon(Offset(900f, 1800f), initialLength = 4, initialLight = 0.7f)
        dragon.resize(Size(1080f, 1920f), Size(200f, 300f))
        assertEquals(4, dragon.length)
        assertEquals(0.7f, dragon.light, 0f)
        assertTrue(dragon.getSegments().all { it.position.x in 48f..152f && it.position.y in 48f..252f })
    }
}
