package com.valerie.yurei.core

import com.valerie.yurei.core.engine.LightBalance
import com.valerie.yurei.core.entity.Dragon
import org.junit.Assert.*
import org.junit.Test

class LightBalanceTest {
    @Test fun initialRestIsGentleAndProlongedRestDecaysFaster() {
        val d = Dragon(initialLight = LightBalance.INITIAL_LIGHT)
        val balance = LightBalance()
        repeat(500) { balance.update(d, 16) }
        assertEquals(0.784f, d.light, 0.0001f)
        repeat(500) { balance.update(d, 16) }
        assertEquals(0.720f, d.light, 0.0001f)
    }

    @Test fun noviceStillHasLightAfterOneMinuteWithoutCollecting() {
        val d = Dragon(initialLight = LightBalance.INITIAL_LIGHT)
        val balance = LightBalance()
        repeat(3750) { balance.update(d, 16) }
        assertTrue(d.light > 0.35f)
    }

    @Test fun movementRestoresGraceButNotEnergy() {
        val d = Dragon(initialLight = 0.5f)
        val balance = LightBalance(8_000)
        balance.moved(3f)
        balance.update(d, 1000)
        assertEquals(0.498f, d.light, 0.00001f)
    }

    @Test fun lowLightSacrificesOneSegmentAfterEightSeconds() {
        val d = Dragon(initialLength = 3, initialLight = 0.14f)
        val balance = LightBalance(8_000)
        repeat(499) { balance.update(d, 16) }
        assertEquals(3, d.length)
        balance.update(d, 16)
        assertEquals(2, d.length)
        assertTrue(d.light > 0.19f)
    }

    @Test fun lastSegmentCanExpireAndSoulRestoresEnergy() {
        val d = Dragon(initialLight = 0.01f)
        val balance = LightBalance(8_000)
        repeat(100) { balance.update(d, 16) }
        assertEquals(0f, d.light, 0f)
        assertEquals(1, d.length)
        balance.collect(d)
        assertEquals(2, d.length)
        assertEquals(0.12f, d.light, 0.00001f)
    }

    @Test fun restoredTimersContinueWithoutChargingOfflineTime() {
        val d = Dragon(initialLength = 2, initialLight = 0.1f)
        val balance = LightBalance(8_000, 7_984)
        balance.update(d, 16)
        assertEquals(1, d.length)
        assertEquals(0.219872f, d.light, 0.00001f)
    }
}
