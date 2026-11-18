package com.valerie.yurei.core

import com.valerie.yurei.core.maze.Cell
import com.valerie.yurei.core.maze.MazeCatalog
import com.valerie.yurei.core.maze.MazePlan
import org.junit.Assert.*
import org.junit.Test

class MazePlanTest {
    @Test fun readsWallsSoulsAndStart() {
        val plan = MazePlan.parse(
            """
            D.#
            o..
            """
        )
        assertEquals(3, plan.columns)
        assertEquals(2, plan.rows)
        assertEquals(Cell(0, 0), plan.start)
        assertEquals(setOf(Cell(0, 1)), plan.souls)
        assertFalse(plan.isOpen(Cell(2, 0)))
        assertTrue(plan.isOpen(Cell(1, 0)))
        assertTrue("le départ et les âmes sont des couloirs", plan.isOpen(Cell(0, 0)) && plan.isOpen(Cell(0, 1)))
    }

    @Test fun gridBorderActsAsAWall() {
        val plan = MazePlan.parse("D.")
        assertFalse(plan.isOpen(Cell(-1, 0)))
        assertFalse(plan.isOpen(Cell(2, 0)))
        assertFalse(plan.isOpen(Cell(0, -1)))
        assertFalse(plan.isOpen(Cell(0, 1)))
    }

    @Test fun rejectsInvalidPlans() {
        assertThrows(IllegalArgumentException::class.java) { MazePlan.parse("") }
        assertThrows(IllegalArgumentException::class.java) { MazePlan.parse("...") }
        assertThrows(IllegalArgumentException::class.java) { MazePlan.parse("D.D") }
        assertThrows(IllegalArgumentException::class.java) { MazePlan.parse("D.x") }
        assertThrows(IllegalArgumentException::class.java) { MazePlan.parse("D..\n..") }
    }

    @Test fun detectsSoulsBehindWalls() {
        val plan = MazePlan.parse("Do#o")
        assertEquals(setOf(Cell(3, 0)), plan.unreachableSouls())
    }

    @Test fun everyCatalogLevelIsCompletable() {
        assertEquals("identifiants uniques", MazeCatalog.all.size, MazeCatalog.all.map { it.id }.toSet().size)
        MazeCatalog.all.forEach { level ->
            assertEquals("âmes inatteignables dans ${level.id}", emptySet<Cell>(), level.plan.unreachableSouls())
            assertEquals("${level.id} : 9 colonnes", 9, level.plan.columns)
            assertEquals("${level.id} : 16 lignes", 16, level.plan.rows)
        }
        assertEquals(15, MazeCatalog.lake1.plan.souls.size)
        assertSame(MazeCatalog.lake1, MazeCatalog.byId("lac-1"))
        assertNull(MazeCatalog.byId("inconnu"))
    }
}
