package com.valerie.yurei.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.valerie.yurei.core.world.SakuraGarden
import kotlin.math.*

private val blossom = Color(0xFFF5B9D0)
private val bark = Color(0xFF38293E)

internal fun DrawScope.drawSakuraBackground(world: Size, time: Float, chapter: Int) {
    val w = world.width
    val h = world.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF523850), Color(0xFFAB7488), Color(0xFF282A41)), endY = h), size = world)
    val sun = Offset(w * 0.64f, h * 0.18f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF5D5BF).copy(alpha = 0.3f), Color.Transparent), sun, w * 0.38f), w * 0.38f, sun)
    drawCircle(Color(0xFFF5D5BF).copy(alpha = 0.65f), w * 0.085f, sun)
    repeat(4) { layer ->
        val ridge = Path().apply {
            moveTo(0f, h * (0.36f + layer * 0.07f))
            cubicTo(w * 0.3f, h * (0.22f + layer * 0.08f), w * 0.6f, h * (0.5f + layer * 0.03f), w, h * (0.34f + layer * 0.07f))
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(ridge, Color(0xFF292D42).copy(alpha = 0.15f + layer * 0.12f))
    }
    // Raked garden lines and a winding pale path leave a quiet, readable playfield.
    repeat(17) { i ->
        val y = h * (0.55f + i * 0.027f)
        val path = Path().apply {
            moveTo(0f, y)
            cubicTo(w * 0.3f, y - 32f, w * 0.7f, y + 32f, w, y)
        }
        drawPath(path, blossom.copy(alpha = 0.045f), style = Stroke(2f))
    }
    val path = Path().apply {
        moveTo(w * 0.52f, h)
        cubicTo(w * 0.08f, h * 0.76f, w * 0.91f, h * 0.56f, w * 0.51f, h * 0.29f)
    }
    drawPath(path, Color(0xFFE5C5B4).copy(alpha = 0.08f), style = Stroke(w * 0.16f))
    // Framing branches are decorative; only the outlined stones are hazardous.
    for (side in listOf(-1, 1)) {
        val rootX = if (side == -1) -w * 0.03f else w * 1.03f
        val tipX = if (side == -1) w * 0.43f else w * 0.68f
        val tree = Path().apply {
            moveTo(rootX, h * 0.60f)
            cubicTo(rootX - side * w * 0.12f, h * 0.34f, rootX + side * w * 0.04f, h * 0.16f, tipX, h * 0.08f)
        }
        drawPath(tree, bark, style = Stroke(w * 0.036f, cap = StrokeCap.Round))
        repeat(5) { i ->
            val start = Offset(rootX - side * w * (0.025f + i * 0.028f), h * (0.37f - i * 0.055f))
            val end = start + Offset(-side * w * 0.18f, -h * 0.065f)
            drawLine(bark, start, end, w * (0.012f - i * 0.0015f), StrokeCap.Round)
            repeat(16) { n ->
                val a = n * 2.4f
                val radius = sqrt(n.toFloat()) * w * 0.018f
                val center = end + Offset(cos(a) * radius, sin(a) * radius * 0.65f)
                drawCircle(if (n % 3 == 0) Color(0xFFD58DAE) else blossom.copy(alpha = 0.85f), w * (0.018f + n % 3 * 0.005f), center)
            }
        }
    }
    SakuraGarden.stones(world).forEach { stone ->
        val p = stone.center
        val r = stone.radius
        drawOval(Color.Black.copy(alpha = 0.18f), p + Offset(-r * 1.2f, r * 0.6f), Size(r * 2.4f, r * 0.7f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF88808F), Color(0xFF4C455D)), p - Offset(r * 0.3f, r * 0.3f), r * 1.5f), r, p)
        drawCircle(Color(0xFFE9B6C1).copy(alpha = 0.9f), r, p, style = Stroke(3f))
        drawLine(Color(0xFFB8B1AF).copy(alpha = 0.6f), p + Offset(-r * 0.55f,-r * 0.25f), p + Offset(r * 0.3f,-r * 0.5f), 3f)
        repeat(5) { i -> drawCircle(blossom.copy(alpha = 0.6f), 4f, p + Offset((i - 2) * 10f, r * 0.4f + sin(i.toFloat()) * 8f)) }
    }
    repeat(34 + chapter * 7) { i ->
        val x = ((i * 173f + time * (17 + i % 7) + sin(time * 0.6f + i) * 35f) % w + w) % w
        val y = ((i * 251f + time * (26 + i % 11)) % h + h) % h
        val radius = 4f + i % 5
        drawOval(blossom.copy(alpha = 0.35f + i % 3 * 0.12f), Offset(x,y), Size(radius * 1.8f, radius * (0.6f + abs(sin(time + i)))))
    }
}

internal fun DrawScope.drawSakuraMemory() {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.5f, h * 0.48f)
    drawCircle(Brush.radialGradient(listOf(blossom.copy(alpha = 0.22f), Color.Transparent), center, w * 0.48f), w * 0.48f, center)
    drawCircle(blossom.copy(alpha = 0.3f), w * 0.36f, center, style = Stroke(w * 0.009f))
    val branch = Path().apply {
        moveTo(w * 0.15f,h * 0.72f)
        cubicTo(w * 0.28f,h * 0.4f,w * 0.15f,h * 0.25f,w * 0.72f,h * 0.2f)
    }
    drawPath(branch, Color(0xFFB8879F), style = Stroke(w * 0.02f, cap = StrokeCap.Round))
    repeat(24) { i ->
        val x = w * (0.24f + (i % 8) * 0.065f)
        val y = h * (0.16f + i / 8 * 0.08f) + sin(i * 1.8f) * h * 0.025f
        drawCircle(blossom.copy(alpha = 0.85f), w * 0.025f, Offset(x,y))
    }
    val figure = Offset(w * 0.42f,h * 0.56f)
    drawCircle(Color(0xFFF0E7D5),w * 0.03f,figure)
    val robe = Path().apply { moveTo(figure.x,figure.y + w * 0.03f); lineTo(w * 0.35f,h * 0.86f); lineTo(w * 0.49f,h * 0.86f); close() }
    drawPath(robe,Color(0xFFCDA6B7))
    val dragon = Path().apply { moveTo(w * 0.82f,h * 0.4f); cubicTo(w * 0.55f,h * 0.3f,w * 0.87f,h * 0.83f,w * 0.60f,h * 0.65f) }
    drawPath(dragon,Color(0xFFB3DCD9), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
    drawOval(Color(0xFFE7EFE8),Offset(w * 0.55f,h * 0.60f),Size(w * 0.09f,h * 0.07f))
    drawLine(Color(0xFFF0E7D5),figure + Offset(0f,w * 0.06f),Offset(w * 0.54f,h * 0.62f),w * 0.012f)
    drawCircle(blossom,w * 0.018f,Offset(w * 0.54f,h * 0.62f))
}
