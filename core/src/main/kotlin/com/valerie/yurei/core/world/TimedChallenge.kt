package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.entity.Dragon
import kotlin.math.*

/** Simulation time only: pausing never consumes the deadline or collision grace. */
class TimedChallenge(
    var remainingMs: Long = 180_000,
    var collected: Int = 0,
    var graceMs: Long = 0,
    var heading: Float = -PI.toFloat() / 2,
    var desiredHeading: Float = heading
) {
    val won get() = collected >= 20 && remainingMs > 0
    val lost get() = remainingMs <= 0

    fun steer(dx: Float, dy: Float) {
        if (dx.isFinite() && dy.isFinite() && hypot(dx, dy) > 0.5f) desiredHeading = atan2(dy, dx)
    }

    fun move(dragon: Dragon, size: Size, dt: Long) {
        val difference = atan2(sin(desiredHeading - heading), cos(desiredHeading - heading))
        heading += difference.coerceIn(-dt * 0.0028f, dt * 0.0028f)
        val step = size.minDimension * 0.22f * dt / 1000f
        val p = dragon.headPosition
        val next = p + Offset(cos(heading), sin(heading)) * step
        // The shore reflects the dragon; scenery is not lethal in level one.
        val margin = min(48f, size.minDimension / 2)
        if (next.x < margin || next.x > size.width - margin) {
            heading = PI.toFloat() - heading; desiredHeading = heading
        }
        if (next.y < margin || next.y > size.height - margin) {
            heading = -heading; desiredHeading = heading
        }
        dragon.moveBy(cos(heading) * step, sin(heading) * step, size)
    }

    fun tick(dt: Long, souls: Int, touchingTail: Boolean): Boolean {
        if (won || lost || dt <= 0) return false
        remainingMs = (remainingMs - dt).coerceAtLeast(0)
        graceMs = (graceMs - dt).coerceAtLeast(0)
        val hit = touchingTail && graceMs == 0L
        if (hit) {
            remainingMs = (remainingMs - 10_000).coerceAtLeast(0)
            graceMs = 2_000
        }
        // Expiry wins ties: collecting on an already expired frame cannot rescue a run.
        if (!lost) collected = (collected + souls).coerceAtMost(20)
        return hit
    }

    fun touchesTail(dragon: Dragon): Boolean {
        val points = dragon.getSegments().map { it.position }
        val head = dragon.headPosition
        return (5 until points.lastIndex).any { i ->
            val a = points[i]; val ab = points[i + 1] - a
            val lengthSquared = ab.x * ab.x + ab.y * ab.y
            val ah = head - a
            val t = if (lengthSquared == 0f) 0f else ((ah.x * ab.x + ah.y * ab.y) / lengthSquared).coerceIn(0f, 1f)
            (head - (a + ab * t)).getDistance() < 24f
        }
    }
}
