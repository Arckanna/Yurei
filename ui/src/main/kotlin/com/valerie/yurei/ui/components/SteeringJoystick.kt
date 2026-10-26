package com.valerie.yurei.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Fixed origin for each touch: small movements back toward the origin do not reverse steering. */
@Composable
fun SteeringJoystick(modifier: Modifier = Modifier, onDirection: (Float, Float) -> Unit) {
    var origin by remember { mutableStateOf<Offset?>(null) }
    var displacement by remember { mutableStateOf(Offset.Zero) }
    val currentDirection by rememberUpdatedState(onDirection)
    Canvas(modifier.testTag("steering-joystick")
        .semantics { contentDescription = "Joystick de direction. Maintenez puis écartez le doigt du centre. Relâchez pour conserver le cap." }
        .pointerInput(Unit) {
            val deadZone = 14.dp.toPx()
            try {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val anchor = down.position
                    origin = anchor
                    displacement = Offset.Zero
                    try {
                        do {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                            if (pointer == null || !pointer.pressed) break
                            if (pointer.isConsumed) break
                            pointer.consume()
                            displacement = pointer.position - anchor
                            if (displacement.getDistance() >= deadZone) {
                                currentDirection(displacement.x, displacement.y)
                            }
                        } while (true)
                    } finally {
                        origin = null
                        displacement = Offset.Zero
                    }
                }
            } finally { origin = null }
        }) {
        origin?.let { anchor ->
            val radius = 54.dp.toPx()
            val distance = displacement.getDistance()
            val active = distance >= 14.dp.toPx()
            val tint = if (active) Color(0xFFE6C68A) else Color(0xFFF0E7D5)
            val knob = anchor + displacement * if (distance > radius) radius / distance else 1f
            drawCircle(Color(0xFF101D30).copy(alpha = 0.45f), radius, anchor)
            drawCircle(tint.copy(alpha = 0.5f), radius, anchor, style = Stroke(1.5.dp.toPx()))
            drawCircle(tint.copy(alpha = 0.18f), 14.dp.toPx(), anchor)
            if (active) drawLine(tint.copy(alpha = 0.7f), anchor, knob, 2.dp.toPx())
            drawCircle(tint.copy(alpha = 0.85f), 10.dp.toPx(), knob)
        }
    }
}
