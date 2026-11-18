package com.valerie.yurei.core

import com.valerie.yurei.core.maze.Direction
import com.valerie.yurei.core.maze.swipeDirection
import org.junit.Assert.*
import org.junit.Test

/** Glissements du doigt (REGLES_LABYRINTHE.md §3.3), y vers le bas comme à l'écran. */
class MazeSwipeTest {
    @Test fun shortMovesAreIgnored() {
        assertNull(swipeDirection(0f, 0f, 40f))
        assertNull(swipeDirection(39f, -39f, 40f))
    }

    @Test fun theDominantAxisGivesTheDirection() {
        assertEquals(Direction.Right, swipeDirection(60f, 20f, 40f))
        assertEquals(Direction.Left, swipeDirection(-60f, -20f, 40f))
        assertEquals(Direction.Down, swipeDirection(10f, 45f, 40f))
        assertEquals(Direction.Up, swipeDirection(-10f, -45f, 40f))
    }

    @Test fun aPerfectDiagonalCountsAsHorizontal() {
        assertEquals(Direction.Right, swipeDirection(50f, 50f, 40f))
    }

    @Test fun rejectsAnInvalidThreshold() {
        assertThrows(IllegalArgumentException::class.java) { swipeDirection(10f, 0f, 0f) }
    }
}
