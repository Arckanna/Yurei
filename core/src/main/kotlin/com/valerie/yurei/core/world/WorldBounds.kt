package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

fun Size.isPlayable(): Boolean =
    width.isFinite() && height.isFinite() && width > 0f && height > 0f

/** Keep centres inside the surface, including surfaces smaller than the diameter. */
fun Offset.confinedTo(size: Size, radius: Float = 0f): Offset {
    require(size.isPlayable())
    val marginX = radius.coerceIn(0f, size.width / 2f)
    val marginY = radius.coerceIn(0f, size.height / 2f)
    return Offset(
        x.coerceIn(marginX, size.width - marginX),
        y.coerceIn(marginY, size.height - marginY)
    )
}

fun Offset.rescaled(from: Size, to: Size): Offset =
    Offset(x / from.width * to.width, y / from.height * to.height)
