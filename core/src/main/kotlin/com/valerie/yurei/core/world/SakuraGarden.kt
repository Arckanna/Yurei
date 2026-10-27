package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** The same circles drive the visible stones, collision and soul placement. */
object SakuraGarden {
    data class Stone(val center: Offset, val radius: Float)
    val titles = listOf("Le sentier rose", "Le vent des pétales", "La promesse refleurit")

    fun stones(size: Size): List<Stone> = listOf(
        Stone(Offset(size.width * 0.30f, size.height * 0.38f), size.minDimension * 0.065f),
        Stone(Offset(size.width * 0.73f, size.height * 0.59f), size.minDimension * 0.075f),
        Stone(Offset(size.width * 0.36f, size.height * 0.74f), size.minDimension * 0.055f)
    )

    fun touchesStone(head: Offset, radius: Float, size: Size): Boolean =
        stones(size).any { (head - it.center).getDistance() < it.radius + radius }

    fun isSafeForSoul(position: Offset, size: Size): Boolean =
        stones(size).all { (position - it.center).getDistance() >= it.radius + 100f }
}
