package com.valerie.yurei.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.valerie.yurei.core.world.MoonShrine
import kotlin.math.cos
import kotlin.math.sin

private val moonSilver = Color(0xFFDBEAF8)
private val vermilion = Color(0xFFD57860)

internal fun DrawScope.drawMoonBackground(world: Size, timeMs: Long, chapter: Int) {
    val w = world.width
    val h = world.height
    val t = timeMs / 1000f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF101329), Color(0xFF253855), Color(0xFF102638)), endY = h), size = world)
    val moon = Offset(w * .72f, h * .12f)
    drawCircle(Brush.radialGradient(listOf(moonSilver.copy(alpha = .16f), Color.Transparent), moon, w * .3f), w * .3f, moon)
    drawCircle(moonSilver.copy(alpha = .8f), w * .075f, moon)
    drawCircle(Color(0xFF323B58).copy(alpha = .18f), w * .023f, moon + Offset(-w * .025f, w * .018f))
    repeat(32) { i ->
        val p = Offset(w * ((i * 37 % 97) / 97f), h * (.025f + (i * 13 % 29) / 100f))
        drawCircle(moonSilver.copy(alpha = .2f + .1f * sin(t * .4f + i)), 1.5f + i % 3, p)
    }
    // Distant headlands frame a broad, unobstructed bay.
    repeat(3) { i ->
        val ridge = Path().apply {
            moveTo(0f, h * (.26f + i * .04f))
            cubicTo(w * .22f, h * .20f, w * .28f, h * .38f, w * .52f, h * .36f)
            cubicTo(w * .74f, h * .31f, w * .8f, h * .22f, w, h * (.23f + i * .04f))
            lineTo(w, h * .45f); lineTo(0f, h * .45f); close()
        }
        drawPath(ridge, Color(0xFF101B30).copy(alpha = .3f + i * .2f))
    }
    repeat(28) { i ->
        val y = h * (.38f + i * .021f)
        val x = w * (.7f + .035f * sin(i + t * .23f))
        val half = w * (.018f + i * .0028f)
        drawLine(moonSilver.copy(alpha = .045f), Offset(x - half, y), Offset(x + half, y), 2f)
    }
    drawTorii(Offset(w * .5f, h * .245f), w * .29f, .5f)
    drawTorii(Offset(w * .12f, h * .82f), w * .18f, .35f)
    // The geometry of each visible disc is exactly the collision geometry.
    MoonShrine.seals(world).forEach { seal ->
        val phase = MoonShrine.phase(seal, timeMs)
        val p = seal.center
        val r = seal.radius
        when (phase) {
            MoonShrine.Phase.Quiet -> {
                drawCircle(moonSilver.copy(alpha = .18f), r, p, style = Stroke(2f))
                drawCircle(moonSilver.copy(alpha = .2f), 5f, p)
            }
            MoonShrine.Phase.Warning -> {
                val gold = Color(0xFFF1D49B)
                drawCircle(gold.copy(alpha = .10f), r, p)
                drawCircle(gold.copy(alpha = .8f), r, p, style = Stroke(3f))
                drawCircle(gold.copy(alpha = .5f), r * .83f, p, style = Stroke(2f))
                drawArc(gold, -90f, 360f * (MoonShrine.cycleMs(seal, timeMs) - 5000) / 2000f,
                    false, p - Offset(r, r), Size(2 * r, 2 * r), style = Stroke(7f))
            }
            MoonShrine.Phase.Active -> {
                drawCircle(vermilion.copy(alpha = .32f), r, p)
                drawCircle(vermilion, r, p, style = Stroke(4f))
                repeat(8) { i ->
                    val angle = i * Math.PI.toFloat() / 4
                    val v = Offset(cos(angle), sin(angle))
                    drawLine(vermilion.copy(alpha = .75f), p + v * r * .25f, p + v * r * .85f, 3f)
                }
            }
        }
    }
    // Sparse drifting light, kept distinct from the larger collectible souls.
    repeat(14) { i ->
        val p = Offset(w * ((i * .173f + sin(t * .07f + i) * .02f) % 1f),
            h * (.42f + ((i * .137f + t * .008f) % .55f)))
        drawCircle(moonSilver.copy(alpha = .12f + chapter * .025f), 2f, p)
    }
}

private fun DrawScope.drawTorii(center: Offset, width: Float, alpha: Float) {
    val ink = vermilion.copy(alpha = alpha)
    for (sign in listOf(-1, 1)) {
        drawLine(ink, center + Offset(sign * width * .34f, 0f), center + Offset(sign * width * .39f, width * .72f), width * .055f)
    }
    drawLine(ink, center - Offset(width * .45f, 0f), center + Offset(width * .45f, 0f), width * .075f, StrokeCap.Round)
    drawLine(ink, center + Offset(-width * .43f, width * .17f), center + Offset(width * .43f, width * .17f), width * .045f)
    val roof = Path().apply {
        moveTo(center.x - width * .57f, center.y - width * .1f)
        quadraticTo(center.x, center.y + width * .01f, center.x + width * .57f, center.y - width * .1f)
    }
    drawPath(roof, ink, style = Stroke(width * .065f, cap = StrokeCap.Round))
}

internal fun DrawScope.drawMoonMemory() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1B2441), Color(0xFF102638))))
    drawCircle(moonSilver.copy(alpha = .8f), w * .09f, Offset(w * .78f, h * .18f))
    drawTorii(Offset(w * .5f, h * .22f), w * .6f, .8f)
    val bell = Offset(w * .5f, h * .39f)
    drawOval(Color(0xFFE7C995), bell - Offset(w * .045f, h * .055f), Size(w * .09f, h * .11f))
    drawLine(moonSilver.copy(alpha = .7f), bell, Offset(w * .49f, h * .78f), 2f)
    for ((x, scale) in listOf(.38f to 1f, .61f to .7f)) {
        val head = Offset(w * x, h * (.66f + (1 - scale) * .2f))
        drawCircle(moonSilver.copy(alpha = .85f), w * .025f * scale, head)
        drawLine(moonSilver.copy(alpha = .75f), head + Offset(0f, w * .03f), Offset(head.x, h * .94f), w * .045f * scale, StrokeCap.Round)
        drawLine(moonSilver.copy(alpha = .8f), head + Offset(0f, h * .08f), Offset(w * .49f, h * .76f), w * .012f, StrokeCap.Round)
    }
    repeat(3) { i ->
        drawArc(moonSilver.copy(alpha = .15f - i * .035f), 195f, 150f, false,
            bell - Offset(w * (.10f + i * .045f), h * (.14f + i * .06f)),
            Size(w * (.20f + i * .09f), h * (.28f + i * .12f)), style = Stroke(1.5f))
    }
}
