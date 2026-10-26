package com.valerie.yurei.data

import com.valerie.yurei.data.model.*
import org.junit.Assert.*
import org.junit.Test

class SavedGameTest {
    private val game = SavedGame(
        width = 300f, height = 500f,
        segments = listOf(SavedPoint(150f, 250f), SavedPoint(140f, 240f)),
        light = 0.42f, score = 30,
        souls = listOf(SavedSoul(7, SavedPoint(80f, 80f), 20f, 0.8f, 1.3f)),
        fog = listOf(SavedFog(SavedPoint(20f, 50f), 60f, 0.2f, 0.3f, 0.4f)),
        worldTimeMs = 54_321, idleMs = 1234, erosionMs = 456
    )

    @Test fun fullStateRoundTripsExactly() {
        assertEquals(game, SavedGameCodec.decode(SavedGameCodec.encode(game)))
    }

    @Test fun timedSavePreservesDeadlineDirectionAndGrace() {
        val timed = game.copy(version = 3, remainingMs = 65432, collected = 12, graceMs = 1200, heading = 1.2f, desiredHeading = 2.1f)
        assertEquals(timed, SavedGameCodec.decode(SavedGameCodec.encode(timed)))
        assertFalse(timed.copy(remainingMs = 0).isValid())
        assertFalse(timed.copy(completed = true).isValid())
        assertTrue(timed.copy(collected = 20, completed = true).isValid())
    }

    @Test fun unknownFieldsAreAccepted() {
        val text = SavedGameCodec.encode(game).replaceFirst("{", "{\"future\":true,")
        assertEquals(game, SavedGameCodec.decode(text))
    }

    @Test fun legacySaveStartsTheJourneyWithoutLosingTheDragon() {
        val old = """{"version":1,"width":300.0,"height":500.0,"segments":[{"x":150.0,"y":250.0}],"light":0.42,"score":30,"souls":[],"fog":[],"worldTimeMs":1000,"idleMs":0,"erosionMs":0}"""
        val loaded = SavedGameCodec.decode(old)
        assertEquals(30, loaded.score)
        assertEquals(0, loaded.chapter)
        assertFalse(loaded.completed)
        assertTrue(loaded.journeyStarted)
    }

    @Test fun completedMemoryRoundTripsAndInvalidEndingIsRejected() {
        val complete = game.copy(chapter = 2, chapterMs = 60_000, offerings = 4, journeyMs = 180_000, completed = true)
        assertEquals(complete, SavedGameCodec.decode(SavedGameCodec.encode(complete)))
        assertFalse(complete.copy(chapter = 1).isValid())
        assertFalse(complete.copy(offerings = 0).isValid())
        assertFalse(complete.copy(journeyMs = 10).isValid())
    }

    @Test fun invalidStatesAndUnsupportedVersionsAreRejected() {
        listOf(game.copy(width = 0f), game.copy(light = Float.NaN), game.copy(version = 4),
            game.copy(segments = emptyList()), game.copy(idleMs = -1),
            game.copy(souls = game.souls + game.souls)).forEach { assertFalse(it.isValid()) }
        assertThrows(IllegalArgumentException::class.java) { SavedGameCodec.decode("{broken") }
        val unsupported = SavedGameCodec.encode(game).replace("\"version\":2", "\"version\":99")
        assertThrows(IllegalArgumentException::class.java) { SavedGameCodec.decode(unsupported) }
    }
}
