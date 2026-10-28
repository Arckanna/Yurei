package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import com.valerie.yurei.core.entity.Dragon
import kotlin.math.abs
import kotlin.math.hypot

/** Physical head ellipse, excluding decorative light and whiskers. */
internal fun headTouchesCircle(head: Offset, center: Offset, radius: Float): Boolean {
    val x = abs((center.x - head.x).toDouble())
    val y = abs((center.y - head.y).toDouble())
    val a = Dragon.HEAD_HALF_WIDTH.toDouble()
    val b = Dragon.HEAD_HALF_HEIGHT.toDouble()
    if (x > a + radius || y > b + radius) return false
    if (x * x / (a * a) + y * y / (b * b) <= 1.0) return true

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
    return hypot(x - closestX, y - closestY) < radius
}
