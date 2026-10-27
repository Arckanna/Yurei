package com.valerie.yurei.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.valerie.yurei.ui.viewmodel.GameUiState
import kotlin.math.sin
import kotlin.math.cos

private val silver = Color(0xFFE7EFE8)
private val lanternGold = Color(0xFFF1CE8D)

/** Layered, resolution-independent ink landscape. All motion uses saved play time. */
internal fun DrawScope.drawLanternWorld(ui: GameUiState, worldSize: Size = size) {
    val w = worldSize.width
    val h = worldSize.height
    val time = ui.journeyMs / 1000f
    val lanternGold = if (ui.level == 2) Color(0xFFFFD6E5) else Color(0xFFF1CE8D)
    if (ui.level == 2) {
        drawSakuraBackground(worldSize, time, ui.chapter)
    } else {
    val palettes = listOf(Color(0xFF25344C), Color(0xFF365665), Color(0xFF787C8D))
    val blend = (ui.chapterMs / 6_000f).coerceIn(0f, 1f)
    val sky = lerp(palettes[(ui.chapter - 1).coerceAtLeast(0)], palettes[ui.chapter], blend)
    drawRect(Brush.verticalGradient(listOf(sky, Color(0xFF152D3D), Color(0xFF102536)), endY = h), size = worldSize)
    val moon = Offset(w * 0.75f, h * 0.13f)
    drawCircle(Brush.radialGradient(listOf(silver.copy(alpha = 0.13f), Color.Transparent), moon, w * 0.3f), w * 0.3f, moon)
    drawCircle(silver.copy(alpha = 0.65f), w * 0.07f, moon)
    repeat(4) { layer ->
        val base = h * (0.3f + layer * 0.06f)
        val ridge = Path().apply {
            moveTo(-w * 0.1f, base)
            cubicTo(w * 0.12f, base - h * (0.15f - layer * 0.02f), w * 0.2f, base - h * 0.04f, w * 0.34f, base)
            cubicTo(w * 0.55f, base - h * 0.2f, w * 0.68f, base - h * 0.11f, w * 1.1f, base + h * 0.02f)
            lineTo(w, h * 0.57f); lineTo(0f, h * 0.57f); close()
        }
        drawPath(ridge, Color(0xFF101F31).copy(alpha = 0.18f + layer * 0.12f))
    }
    // Lake reflections: thin imperfect horizontal strokes, never a bright grid.
    repeat(20) { i ->
        val y = h * (0.46f + i * 0.026f)
        val centre = w * (0.5f + sin(i * 1.7f + time * 0.08f) * 0.15f)
        val half = w * (0.08f + (i % 5) * 0.035f)
        drawLine(silver.copy(alpha = 0.025f + (i % 3) * 0.012f), Offset(centre - half, y), Offset(centre + half, y), 1.5f)
    }
    // A distant gate anchors the shoreline.
    val gateX = w * 0.17f
    val gateY = h * 0.4f
    val gateWidth = w * 0.17f
    val ink = Color(0xFF0C1A29)
    drawLine(ink, Offset(gateX - gateWidth / 2, gateY), Offset(gateX + gateWidth / 2, gateY), w * 0.014f, StrokeCap.Round)
    drawLine(ink, Offset(gateX - gateWidth * 0.6f, gateY - h * 0.02f), Offset(gateX + gateWidth * 0.6f, gateY - h * 0.02f), w * 0.018f, StrokeCap.Round)
    for (sign in listOf(-1, 1)) drawLine(ink, Offset(gateX + sign * gateWidth * 0.34f, gateY - h * 0.02f),
        Offset(gateX + sign * gateWidth * 0.4f, gateY + h * 0.09f), w * 0.014f)

    }
    ui.fogParticles.forEach { fog ->
        val radius = fog.size * 2f
        drawCircle(Brush.radialGradient(listOf(silver.copy(alpha = fog.opacity * (0.23f - ui.chapter * 0.04f)), Color.Transparent),
            fog.position, radius), radius, fog.position)
    }
    if (ui.level == 1 && ui.refugeRadius > 0) {
        val p = ui.refuge
        val pulse = 1f + 0.04f * sin(time)
        drawCircle(Brush.radialGradient(listOf(lanternGold.copy(alpha = if (ui.sheltered) 0.24f else 0.12f), Color.Transparent),
            p, ui.refugeRadius * 1.7f), ui.refugeRadius * 1.7f, p)
        drawCircle(lanternGold.copy(alpha = 0.35f), ui.refugeRadius * pulse, p, style = Stroke(1.5f))
        drawLantern(p, w * 0.04f)
        // Four petals around the refuge show offerings, without a numeric HUD.
        repeat(4) { i ->
            val angle = i * Math.PI.toFloat() / 2 - Math.PI.toFloat() / 2
            drawCircle(lanternGold.copy(alpha = if (i < ui.offerings) 0.95f else 0.18f), w * 0.008f,
                p + Offset(cos(angle), sin(angle)) * ui.refugeRadius * 1.2f)
        }
    }
    ui.souls.forEach { soul ->
        val r = soul.radius
        drawCircle(Brush.radialGradient(listOf(lanternGold.copy(alpha = 0.3f), Color.Transparent), soul.position, r * 2.5f), r * 2.5f, soul.position)
        drawCircle(lanternGold.copy(alpha = soul.lightIntensity), r * 0.65f, soul.position)
        drawCircle(silver, r * 0.18f, soul.position)
    }
    val segments = ui.dragon.segments
    if (segments.isNotEmpty()) {
        val light = segments.first().light.coerceAtLeast(0.25f)
        val ribbon = Path().apply {
            moveTo(segments.first().position.x, segments.first().position.y)
            for (i in 1 until segments.size) {
                val a = segments[i - 1].position
                val b = segments[i].position
                quadraticTo(a.x, a.y, (a.x + b.x) / 2, (a.y + b.y) / 2)
            }
        }
        drawPath(ribbon, silver.copy(alpha = light * 0.12f), style = Stroke(38f, cap = StrokeCap.Round))
        drawPath(ribbon, Color(0xFFB3DCD9).copy(alpha = light), style = Stroke(15f, cap = StrokeCap.Round))
        segments.drop(1).forEachIndexed { i, segment ->
            drawCircle(silver.copy(alpha = light * 0.3f), (9f - i * 0.2f).coerceAtLeast(3f), segment.position)
        }
        val head = segments.first().position
        val r = ui.dragon.headRadius * 0.6f
        drawCircle(Brush.radialGradient(listOf(silver.copy(alpha = 0.18f), Color.Transparent), head, r * 3f), r * 3f, head)
        drawOval(silver.copy(alpha = light), head - Offset(r, r * 0.65f), Size(r * 2f, r * 1.3f))
        for (sign in listOf(-1f, 1f)) {
            val horn = Path().apply {
                moveTo(head.x - r * 0.4f, head.y + sign * r * 0.4f)
                quadraticTo(head.x - r, head.y + sign * r * 1.5f, head.x - r * 1.5f, head.y + sign * r * 1.3f)
            }
            drawPath(horn, silver.copy(alpha = light), style = Stroke(3f, cap = StrokeCap.Round))
            drawCircle(Color(0xFF28434E), 3f, head + Offset(r * 0.35f, sign * r * 0.25f))
            val whisker = Path().apply {
                moveTo(head.x + r * 0.8f, head.y + sign * r * 0.3f)
                quadraticTo(head.x + r * 1.7f, head.y + sign * r, head.x + r * 2f, head.y + sign * r * 0.4f)
            }
            drawPath(whisker, silver.copy(alpha = light * 0.6f), style = Stroke(1.5f))
        }
    }
    // An ensō-like arc fills slowly as the chapter breathes.
    val arcSize = w * 0.1f
    drawArc(lanternGold.copy(alpha = 0.65f), -90f, 360f * (ui.chapterMs / 60_000f).coerceAtMost(1f), false,
        Offset(w - arcSize - 20f, h - arcSize - 20f), Size(arcSize, arcSize), style = Stroke(2f))
}

private fun DrawScope.drawLantern(p: Offset, radius: Float) {
    drawOval(lanternGold.copy(alpha = 0.9f), p - Offset(radius * 0.7f, radius), Size(radius * 1.4f, radius * 2f))
    repeat(3) { i ->
        val y = p.y - radius * 0.5f + i * radius * 0.5f
        drawLine(Color(0xFF7C684F).copy(alpha = 0.5f), Offset(p.x - radius * 0.65f, y), Offset(p.x + radius * 0.65f, y), 1.5f)
    }
    drawLine(lanternGold, p - Offset(0f, radius), p - Offset(0f, radius * 1.4f), 2f)
    drawCircle(silver.copy(alpha = 0.8f), radius * 0.2f, p)
}

internal fun DrawScope.drawLanternMemory() {
    val w = size.width
    val h = size.height
    val light = Offset(w * 0.5f, h * 0.48f)
    drawCircle(Brush.radialGradient(listOf(lanternGold.copy(alpha = 0.3f), Color.Transparent), light, w * 0.5f), w * 0.5f, light)
    drawCircle(lanternGold.copy(alpha = 0.28f), w * 0.34f, light, style = Stroke(w * 0.012f))
    // Small figure, outstretched arm, and a dragon bowing towards the gift.
    val figure = Offset(w * 0.28f, h * 0.43f)
    drawCircle(Color(0xFFBACCC7), w * 0.035f, figure)
    val robe = Path().apply {
        moveTo(figure.x, figure.y + w * 0.04f)
        lineTo(figure.x - w * 0.06f, h * 0.77f)
        lineTo(figure.x + w * 0.07f, h * 0.77f); close()
    }
    drawPath(robe, Color(0xFFBACCC7))
    drawLine(silver, figure + Offset(0f, w * 0.07f), light - Offset(w * 0.03f, 0f), w * 0.018f, StrokeCap.Round)
    val dragon = Path().apply {
        moveTo(w * 0.86f, h * 0.23f)
        cubicTo(w * 0.55f, h * 0.08f, w * 0.9f, h * 0.75f, w * 0.6f, h * 0.5f)
    }
    drawPath(dragon, Color(0xFFB3DCD9), style = Stroke(w * 0.035f, cap = StrokeCap.Round))
    drawOval(silver, Offset(w * 0.56f, h * 0.44f), Size(w * 0.09f, h * 0.07f))
    drawLine(silver, Offset(w * 0.63f, h * 0.46f), Offset(w * 0.67f, h * 0.38f), w * 0.008f)
    drawLantern(light, w * 0.035f)
    repeat(6) { i ->
        val y = h * (0.8f + i * 0.026f)
        drawLine(lanternGold.copy(alpha = 0.25f - i * 0.03f), Offset(w * (0.34f + i * 0.01f), y),
            Offset(w * (0.7f - i * 0.01f), y), 2f)
    }
}
