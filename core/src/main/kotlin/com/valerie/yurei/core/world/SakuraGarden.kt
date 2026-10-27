package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.entity.Dragon
import kotlin.math.abs
import kotlin.math.hypot

/** The same circles drive the visible stones, collision and soul placement. */
object SakuraGarden {
    data class Stone(val center: Offset, val radius: Float)
    val titles = listOf("Le sentier rose", "Le vent des pétales", "La promesse refleurit")

    fun stones(size: Size): List<Stone> = listOf(
        Stone(Offset(size.width * 0.30f, size.height * 0.38f), size.minDimension * 0.065f),
        Stone(Offset(size.width * 0.73f, size.height * 0.59f), size.minDimension * 0.075f),
        Stone(Offset(size.width * 0.36f, size.height * 0.74f), size.minDimension * 0.055f)
    )

    fun touchesStone(head: Offset, size: Size): Boolean = stones(size).any { stone ->
        val x = abs((stone.center.x - head.x).toDouble())
        val y = abs((stone.center.y - head.y).toDouble())
        val a = Dragon.HEAD_HALF_WIDTH.toDouble()
        val b = Dragon.HEAD_HALF_HEIGHT.toDouble()
        if (x > a + stone.radius || y > b + stone.radius) return@any false
        if (x * x / (a * a) + y * y / (b * b) <= 1.0) return@any true

        // Closest point on the visible ellipse to the stone centre. Solving for
        // the nonnegative Lagrange multiplier also handles both ellipse axes.
        var low = 0.0
        var high = hypot(a * x, b * y)
        repeat(32) {
            val t = (low + high) / 2.0
            val nx = a * x / (t + a * a)
            val ny = b * y / (t + b * b)
            if (nx * nx + ny * ny > 1.0) low = t else high = t
        }
        val closestX = a * a * x / (high + a * a)
        val closestY = b * b * y / (high + b * b)
        hypot(x - closestX, y - closestY) < stone.radius
    }

    fun isSafeForSoul(position: Offset, size: Size): Boolean =
        stones(size).all { (position - it.center).getDistance() >= it.radius + 100f }
}
