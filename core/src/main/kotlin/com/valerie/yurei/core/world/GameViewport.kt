package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** Physics uses this fixed arena. Only rendering converts world units to screen pixels. */
object GameViewport {
    val worldSize = Size(1080f, 1920f)

    fun fit(surface: Size): Transform {
        require(surface.isPlayable())
        val scale = minOf(surface.width / worldSize.width, surface.height / worldSize.height)
        return Transform(scale, Offset(
            (surface.width - worldSize.width * scale) / 2f,
            (surface.height - worldSize.height * scale) / 2f
        ))
    }

    data class Transform(val scale: Float, val offset: Offset) {
        fun toScreen(point: Offset): Offset = offset + point * scale
        fun toWorld(point: Offset): Offset = (point - offset) / scale
    }
}
