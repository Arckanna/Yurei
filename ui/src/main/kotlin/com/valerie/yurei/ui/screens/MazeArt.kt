package com.valerie.yurei.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.valerie.yurei.core.maze.Cell
import com.valerie.yurei.core.maze.Direction
import com.valerie.yurei.ui.viewmodel.MazeUiState
import kotlin.math.sin

/** Taille d'une case en unités du terrain logique : 9 × 120 = 1080, 16 × 120 = 1920. */
internal const val MAZE_CELL = 120f

private val mazeSilver = Color(0xFFE7EFE8)
private val mazeGold = Color(0xFFF1CE8D)
private val mazeJade = Color(0xFFB3DCD9)
private val mazeStone = Color(0xFF0C1A29)

/**
 * Le lac vu d'en haut : l'eau sombre forme les couloirs, des rochers d'encre les murs.
 * Le dessin se fait dans le terrain logique ; l'écran le met à l'échelle.
 */
internal fun DrawScope.drawLakeMaze(ui: MazeUiState) {
    val plan = ui.plan
    val width = plan.columns * MAZE_CELL
    val height = plan.rows * MAZE_CELL
    val time = ui.animationMs / 1000f

    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF25344C), Color(0xFF152D3D), Color(0xFF102536)), endY = height),
        size = Size(width, height)
    )
    // Reflets fins et irréguliers sur l'eau, jamais une grille lumineuse.
    repeat(28) { i ->
        val y = height * (0.02f + i * 0.035f)
        val centre = width * (0.5f + sin(i * 1.7f + time * 0.08f) * 0.3f)
        val half = width * (0.06f + (i % 5) * 0.03f)
        drawLine(mazeSilver.copy(alpha = 0.03f + (i % 3) * 0.012f), Offset(centre - half, y), Offset(centre + half, y), 1.5f)
    }

    // Murs : un rocher par case, aux coins arrondis, avec un liseré d'argent discret.
    val inset = 6f
    for (row in 0 until plan.rows) for (col in 0 until plan.columns) {
        if (plan.isOpen(Cell(col, row))) continue
        val topLeft = Offset(col * MAZE_CELL + inset, row * MAZE_CELL + inset)
        val size = Size(MAZE_CELL - 2 * inset, MAZE_CELL - 2 * inset)
        drawRoundRect(mazeStone.copy(alpha = 0.92f), topLeft, size, CornerRadius(22f))
        drawRoundRect(mazeSilver.copy(alpha = 0.10f), topLeft, size, CornerRadius(22f), style = Stroke(2f))
    }

    // Âmes : elles respirent doucement, chacune à son rythme.
    ui.souls.forEach { soul ->
        val centre = Offset((soul.col + 0.5f) * MAZE_CELL, (soul.row + 0.5f) * MAZE_CELL)
        val r = 22f * (1f + 0.08f * sin(time * 2f + soul.col * 1.3f + soul.row * 0.7f))
        drawCircle(Brush.radialGradient(listOf(mazeGold.copy(alpha = 0.32f), Color.Transparent), centre, r * 2.5f), r * 2.5f, centre)
        drawCircle(mazeGold.copy(alpha = 0.9f), r * 0.65f, centre)
        drawCircle(mazeSilver, r * 0.18f, centre)
    }

    drawMazeDragon(ui, time)
}

private fun DrawScope.drawMazeDragon(ui: MazeUiState, time: Float) {
    val points = ui.body.map { Offset(it.x * MAZE_CELL, it.y * MAZE_CELL) }
    if (points.isEmpty()) return
    // Translucide après une morsure : il respire entre deux opacités, pour qu'on le remarque.
    val alpha = if (ui.translucent) 0.35f + 0.2f * (1f + sin(time * 12f)) / 2f else 1f

    if (points.size > 1) {
        val ribbon = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(ribbon, mazeSilver.copy(alpha = alpha * 0.12f), style = Stroke(70f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(ribbon, mazeJade.copy(alpha = alpha), style = Stroke(34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.drop(1).forEachIndexed { i, p ->
            drawCircle(mazeSilver.copy(alpha = alpha * 0.35f), (12f - i * 0.3f).coerceAtLeast(5f), p)
        }
    }

    val head = points.first()
    val r = 30f
    drawCircle(Brush.radialGradient(listOf(mazeSilver.copy(alpha = 0.2f * alpha), Color.Transparent), head, r * 3f), r * 3f, head)
    // La tête est dessinée vers la droite, puis tournée dans sa direction.
    rotate(headingDegrees(ui.heading), pivot = head) {
        drawOval(mazeSilver.copy(alpha = alpha), head - Offset(r, r * 0.75f), Size(r * 2f, r * 1.5f))
        for (sign in listOf(-1f, 1f)) {
            val horn = Path().apply {
                moveTo(head.x - r * 0.4f, head.y + sign * r * 0.4f)
                quadraticTo(head.x - r, head.y + sign * r * 1.5f, head.x - r * 1.5f, head.y + sign * r * 1.3f)
            }
            drawPath(horn, mazeSilver.copy(alpha = alpha), style = Stroke(4f, cap = StrokeCap.Round))
            drawCircle(Color(0xFF28434E).copy(alpha = alpha), 4.5f, head + Offset(r * 0.35f, sign * r * 0.3f))
            val whisker = Path().apply {
                moveTo(head.x + r * 0.8f, head.y + sign * r * 0.3f)
                quadraticTo(head.x + r * 1.7f, head.y + sign * r, head.x + r * 2f, head.y + sign * r * 0.4f)
            }
            drawPath(whisker, mazeSilver.copy(alpha = alpha * 0.6f), style = Stroke(2f))
        }
    }
}

private fun headingDegrees(direction: Direction) = when (direction) {
    Direction.Right -> 0f
    Direction.Down -> 90f
    Direction.Left -> 180f
    Direction.Up -> 270f
}
