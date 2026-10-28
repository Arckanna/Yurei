package com.valerie.yurei.core

import androidx.compose.ui.geometry.Offset
import com.valerie.yurei.core.world.*
import org.junit.Assert.*
import org.junit.Test

class MoonShrineTest {
    private val size = GameViewport.worldSize
    @Test fun warningPrecedesEveryActivationAndOnlyActiveDiscsHurt() {
        MoonShrine.seals(size).forEach { seal ->
            val cycle = 9000 + seal.offsetMs
            assertEquals(MoonShrine.Phase.Quiet, MoonShrine.phase(seal, cycle + 4999))
            assertEquals(MoonShrine.Phase.Warning, MoonShrine.phase(seal, cycle + 5000))
            assertFalse(MoonShrine.touchesSeal(seal.center, size, cycle + 6999))
            assertEquals(MoonShrine.Phase.Active, MoonShrine.phase(seal, cycle + 7000))
            assertTrue(MoonShrine.touchesSeal(seal.center, size, cycle + 7000))
            assertTrue(MoonShrine.touchesSeal(seal.center, size, cycle + 8999))
            assertFalse(MoonShrine.touchesSeal(seal.center, size, cycle + 9000))
            assertFalse(MoonShrine.touchesSeal(seal.center + Offset(0f, seal.radius + 24f), size, cycle + 7000))
        }
    }
    @Test fun startIsSafeAndAtMostOneSealIsActive() {
        for (time in 0L..180000L step 50) {
            assertFalse(MoonShrine.touchesSeal(Offset(540f, 960f), size, time))
            assertTrue(MoonShrine.seals(size).count { MoonShrine.phase(it, time) == MoonShrine.Phase.Active } <= 1)
        }
    }
    @Test fun soulsRemainOutsideHazardsAndSavedClockKeepsTheirPhase() {
        val world = World(size, level = 3)
        repeat(100) {
            val souls = world.getSouls()
            assertTrue(souls.all { MoonShrine.isSafeForSoul(it.position, size) })
            souls.forEach { world.checkSoulCollisions(it.position, 48f) }
            world.update(50)
        }
        val restored = World(size, initialSoulCount = 0, level = 3)
        restored.restore(world.getSouls(), world.getFogParticles(), world.getTimeMs())
        MoonShrine.seals(size).forEach {
            assertEquals(MoonShrine.phase(it, world.getTimeMs()), MoonShrine.phase(it, restored.getTimeMs()))
        }
    }
}
