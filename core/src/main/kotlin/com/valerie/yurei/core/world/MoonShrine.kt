package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** A nine-second tide: five quiet seconds, two warning seconds, two active seconds.
 * Uses the saved simulation clock, so pausing or restoring never skips a warning. */
object MoonShrine {
    enum class Phase { Quiet, Warning, Active }
    data class Seal(val center: Offset, val radius: Float, val offsetMs: Long)
    val titles = listOf("Les marches du silence", "Le souffle des marées", "La cloche du retour")
    fun seals(size: Size) = listOf(
        Seal(Offset(size.width * .30f, size.height * .38f), size.minDimension * .085f, 0),
        Seal(Offset(size.width * .73f, size.height * .53f), size.minDimension * .085f, 3000),
        Seal(Offset(size.width * .36f, size.height * .68f), size.minDimension * .085f, 6000)
    )
    fun cycleMs(seal: Seal, timeMs: Long): Long = (timeMs.coerceAtLeast(0) - seal.offsetMs).coerceAtLeast(0) % 9000
    fun phase(seal: Seal, timeMs: Long): Phase = when (cycleMs(seal, timeMs)) {
        in 0..4999 -> Phase.Quiet
        in 5000..6999 -> Phase.Warning
        else -> Phase.Active
    }
    fun touchesSeal(head: Offset, size: Size, timeMs: Long) = seals(size).any {
        phase(it, timeMs) == Phase.Active && headTouchesCircle(head, it.center, it.radius)
    }
    // Collectibles never require entering a hazard, even while it sleeps.
    fun isSafeForSoul(position: Offset, size: Size) = seals(size).all {
        (position - it.center).getDistance() >= it.radius + 80f
    }
}
