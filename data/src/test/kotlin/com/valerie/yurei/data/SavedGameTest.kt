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

    @Test fun unknownFieldsAreAccepted() {
        val text = SavedGameCodec.encode(game).replaceFirst("{", "{\"future\":true,")
        assertEquals(game, SavedGameCodec.decode(text))
    }

    @Test fun invalidStatesAndUnsupportedVersionsAreRejected() {
        listOf(game.copy(width = 0f), game.copy(light = Float.NaN), game.copy(version = 2),
            game.copy(segments = emptyList()), game.copy(idleMs = -1),
            game.copy(souls = game.souls + game.souls)).forEach { assertFalse(it.isValid()) }
        assertThrows(IllegalArgumentException::class.java) { SavedGameCodec.decode("{broken") }
        val unsupported = SavedGameCodec.encode(game).replace("\"version\":1", "\"version\":99")
        assertThrows(IllegalArgumentException::class.java) { SavedGameCodec.decode(unsupported) }
    }
}
