package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.engine.LightBalance
import com.valerie.yurei.core.entity.Dragon
import com.valerie.yurei.core.world.LanternJourney
import org.junit.Assert.*
import org.junit.Test

class LanternJourneyTest {
    @Test fun activeJourneyCompletesAtThreeMinutesNotBefore() {
        val journey = LanternJourney()
        repeat(179) { journey.update(1000, 1) }
        assertFalse(journey.completed)
        journey.update(1000, 1)
        assertTrue(journey.completed)
        assertEquals(180_000L, journey.elapsedMs)
        assertEquals(1f, journey.progress(), 0f)
        assertFalse(journey.update(1000, 1))
        assertEquals(180_000L, journey.elapsedMs)
    }

    @Test fun slowCollectorFinishesAtFiveMinutesWithOneOfferingPerChapter() {
        val journey = LanternJourney()
        repeat(3) {
            journey.update(1000, 1)
            repeat(99) { journey.update(1000, 0) }
        }
        assertTrue(journey.completed)
        assertEquals(300_000L, journey.elapsedMs)
    }

    @Test fun idlingCannotUnlockTheMemory() {
        val journey = LanternJourney()
        repeat(400) { journey.update(1000, 0) }
        assertFalse(journey.completed)
        assertEquals(0, journey.chapter)
        assertTrue(journey.update(16, 1))
        assertEquals(1, journey.chapter)
    }

    @Test fun restoredChapterUsesRemainingTimeAndOfferings() {
        val journey = LanternJourney(2, 59_984, 4, 179_984)
        assertTrue(journey.update(16, 0))
        assertTrue(journey.completed)
    }

    @Test fun refugeRestoresLightAndResetsErosion() {
        val size = Size(300f, 500f)
        val journey = LanternJourney()
        val dragon = Dragon(journey.refuge(size), initialLength = 3, initialLight = 0.1f)
        val balance = LightBalance(8000, 7999)
        journey.updateLight(dragon, balance, size, 1000)
        assertEquals(0.125f, dragon.light, 0.00001f)
        assertEquals(3, dragon.length)
        assertEquals(0L, balance.erosionMs)
    }

    @Test fun firstWorldNeverErasesProgressFromExhaustion() {
        val journey = LanternJourney()
        val dragon = Dragon(Offset(150f, 100f), initialLight = 0.1f)
        val balance = LightBalance(8000)
        repeat(20_000) { journey.updateLight(dragon, balance, Size(300f, 500f), 16) }
        assertEquals(0.08f, dragon.light, 0.00001f)
    }
}
