package com.valerie.yurei.core.engine

import com.valerie.yurei.core.entity.Dragon

/** Seconds of play only; pauses and time outside the process never count. */
class LightBalance(var idleMs: Long = 0, var erosionMs: Long = 0) {
    companion object {
        const val INITIAL_LIGHT = 0.8f
        const val SOUL_GAIN = 0.12f
        const val REST_GRACE_MS = 8_000L
        const val EROSION_INTERVAL_MS = 8_000L
    }

    fun moved(distance: Float) {
        if (distance.isFinite() && distance >= 2f) idleMs = 0
    }

    fun update(dragon: Dragon, dtMs: Long) {
        if (dtMs <= 0) return
        val graceMs = (REST_GRACE_MS - idleMs).coerceIn(0L, dtMs)
        idleMs = (idleMs + dtMs).coerceAtMost(REST_GRACE_MS)
        // Moving/recently moving: 0.2%/s. Prolonged rest: 0.8%/s.
        val decay = graceMs * 0.000002f + (dtMs - graceMs) * 0.000008f
        dragon.setLight((dragon.light - decay).coerceAtLeast(0f))
        if (dragon.light <= 0.15f && dragon.length > 1) {
            erosionMs += dtMs
            if (erosionMs >= EROSION_INTERVAL_MS) {
                dragon.removeSegment()
                dragon.setLight((dragon.light + SOUL_GAIN).coerceAtMost(1f))
                erosionMs = 0
            }
        } else erosionMs = 0
    }

    fun collect(dragon: Dragon) {
        dragon.addSegment()
        dragon.setLight((dragon.light + SOUL_GAIN).coerceAtMost(1f))
        erosionMs = 0
    }
}
