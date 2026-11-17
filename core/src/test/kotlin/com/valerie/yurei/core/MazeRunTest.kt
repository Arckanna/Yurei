package com.valerie.yurei.core

import com.valerie.yurei.core.maze.Cell
import com.valerie.yurei.core.maze.Direction
import com.valerie.yurei.core.maze.MazeLevel
import com.valerie.yurei.core.maze.MazePlan
import com.valerie.yurei.core.maze.MazeRun
import org.junit.Assert.*
import org.junit.Test

/**
 * Règles du labyrinthe (REGLES_LABYRINTHE.md §3).
 * Vitesse de test : 4 cases par seconde, soit une case toutes les 250 ms.
 */
class MazeRunTest {
    private fun run(plan: String, lanterns: Int = MazeRun.START_LANTERNS) =
        MazeRun(MazeLevel("test", MazePlan.parse(plan), 4f, Direction.Right), lanterns)

    /** Avance comme la boucle de jeu : par pas de 50 ms. */
    private fun MazeRun.advance(ms: Long) = repeat((ms / 50).toInt()) { update(50) }

    @Test fun waitsForTheFirstSwipe() {
        val game = run("D..o")
        game.advance(1_000)
        assertFalse(game.moving)
        assertEquals(Cell(0, 0), game.head)
        assertEquals("le temps ne compte pas à l'arrêt", 0L, game.elapsedMs)
    }

    @Test fun movesOneCellPerStepAndShowsProgress() {
        val game = run("D...o")
        game.swipe(Direction.Right)
        game.advance(100)
        assertEquals(Cell(0, 0), game.head)
        assertEquals(0.4f, game.stepProgress, 0.001f)
        game.advance(150)
        assertEquals(Cell(1, 0), game.head)
        game.advance(250)
        assertEquals(Cell(2, 0), game.head)
    }

    @Test fun stopsInFrontOfAWallWithoutPenalty() {
        val game = run(
            """
            D..
            o##
            """
        )
        game.swipe(Direction.Right)
        game.advance(1_000)
        assertEquals(Cell(2, 0), game.head)
        assertFalse(game.moving)
        assertEquals(MazeRun.START_LANTERNS, game.lanterns)
        assertEquals("le temps s'arrête avec le dragon", 500L, game.elapsedMs)

        game.swipe(Direction.Down) // mur : rien ne se passe
        assertFalse(game.moving)
        game.swipe(Direction.Left) // pas encore de corps : le demi-tour est permis
        assertTrue(game.moving)
    }

    @Test fun remembersATurnUntilAJunctionAllowsIt() {
        val game = run(
            """
            D...o
            ##.##
            ##...
            """
        )
        game.swipe(Direction.Right)
        game.swipe(Direction.Down) // trop tôt : mur sous la case suivante
        game.advance(250)
        assertEquals(Cell(1, 0), game.head)
        assertEquals("tout droit, virage gardé en mémoire", Direction.Right, game.heading)
        assertEquals(Direction.Down, game.queued)
        game.advance(250)
        assertEquals(Cell(2, 0), game.head)
        assertEquals(Direction.Down, game.heading)
        assertNull(game.queued)
        game.advance(250)
        assertEquals(Cell(2, 1), game.head)
    }

    @Test fun eatingASoulGrowsTheBodyAndScores() {
        val game = run("Do..o")
        game.swipe(Direction.Right)
        game.advance(250)
        assertEquals(listOf(Cell(1, 0), Cell(0, 0)), game.body)
        assertEquals(MazeRun.SOUL_POINTS, game.score)
        assertEquals(setOf(Cell(4, 0)), game.remainingSouls)
        game.advance(250)
        assertEquals("la longueur reste acquise", listOf(Cell(2, 0), Cell(1, 0)), game.body)
    }

    @Test fun directUTurnIsIgnoredOnceTheDragonHasABody() {
        val game = run("Do...o")
        game.swipe(Direction.Right)
        game.advance(250)
        game.swipe(Direction.Left)
        assertNull(game.queued)
        game.advance(250)
        assertEquals(Cell(2, 0), game.head)
        assertEquals(Direction.Right, game.heading)
    }

    @Test fun enteringTheCellTheTailLeavesIsNotABite() {
        val game = run(
            """
            Do
            oo
            .o
            """
        )
        game.swipe(Direction.Right)
        game.swipe(Direction.Down)
        game.advance(250) // (1,0)
        game.swipe(Direction.Left)
        game.advance(250) // (1,1)
        game.swipe(Direction.Up)
        game.advance(250) // (0,1), longueur 4
        game.advance(250) // (0,0) : la queue vient d'en partir
        assertEquals(Cell(0, 0), game.head)
        assertEquals(MazeRun.START_LANTERNS, game.lanterns)
    }

    /** Dragon de 5 segments qui fait demi-tour en carré et retombe sur son corps. */
    private fun biteScenario(lanterns: Int = MazeRun.START_LANTERNS): MazeRun {
        val game = run(
            """
            Doooo.
            ......
            .....o
            """,
            lanterns
        )
        game.swipe(Direction.Right)
        game.advance(750)          // tête en (3,0)
        game.swipe(Direction.Down)
        game.advance(250)          // (4,0), longueur 5, part vers le bas
        game.swipe(Direction.Left)
        game.advance(250)          // (4,1)
        game.swipe(Direction.Up)
        game.advance(250)          // (3,1), regarde vers (3,0) où se trouve le corps
        game.advance(250)          // morsure
        return game
    }

    @Test fun bitingTheTailCostsALanternStopsAndProtects() {
        val game = biteScenario()
        assertEquals(MazeRun.START_LANTERNS - 1, game.lanterns)
        assertFalse(game.moving)
        assertEquals(Cell(3, 1), game.head)
        assertTrue(game.translucent)
        assertEquals("les âmes recueillies restent acquises", setOf(Cell(5, 2)), game.remainingSouls)

        game.advance(5_000)
        assertEquals("la protection ne s'use pas à l'arrêt", MazeRun.GRACE_MS, game.graceMs)

        game.swipe(Direction.Up) // traverse son corps grâce à la protection
        game.advance(250)
        assertEquals(Cell(3, 0), game.head)
        assertEquals(MazeRun.START_LANTERNS - 1, game.lanterns)
        assertEquals(MazeRun.GRACE_MS - 250, game.graceMs)
    }

    @Test fun losingTheLastLanternEndsTheLevel() {
        val game = biteScenario(lanterns = 1)
        assertTrue(game.lost)
        assertTrue(game.finished)
        assertFalse(game.translucent)
        game.swipe(Direction.Left)
        assertFalse("plus aucun mouvement après l'échec", game.moving)
    }

    @Test fun collectingEverySoulWinsWithLevelPointsAndTimeBonus() {
        val game = run("Doo")
        game.swipe(Direction.Right)
        game.advance(500)
        assertTrue(game.won)
        assertFalse(game.moving)
        // 2 âmes + tableau + 89 secondes restantes sous 90 s.
        assertEquals(2 * 10 + 500 + 89 * 10, game.score)
    }

    @Test fun timeBonusShrinksAndNeverGoesNegative() {
        assertEquals(900, MazeRun.timeBonus(0))
        assertEquals(590, MazeRun.timeBonus(30_500))
        assertEquals(0, MazeRun.timeBonus(89_999))
        assertEquals(0, MazeRun.timeBonus(120_000))
    }
}
