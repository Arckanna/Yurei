package com.valerie.yurei

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.compose.setContent
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.valerie.yurei.ui.components.SteeringJoystick
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SteeringJoystickTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun smallCorrectionsKeepDirectionAndEachTouchGetsANewOrigin() {
        val commands = mutableListOf<Offset>()
        var density = 1f
        compose.activityRule.scenario.onActivity { activity ->
        activity.setContent {
            density = LocalDensity.current.density
            SteeringJoystick(Modifier.size(240.dp)) { x, y -> commands.add(Offset(x, y)) }
        }
        }
        compose.waitForIdle()
        val pad = compose.onNodeWithTag("steering-joystick")
        pad.performTouchInput {
            down(center)
            moveTo(center + Offset(5 * density, 0f))
        }
        compose.runOnIdle { assertTrue(commands.isEmpty()) }
        pad.performTouchInput {
            moveTo(center + Offset(40 * density, 0f))
            // Moving left while still right of the anchor must continue to steer right.
            moveTo(center + Offset(30 * density, 0f))
        }
        compose.runOnIdle {
            assertTrue(commands.isNotEmpty())
            assertTrue(commands.all { it.x > 0 && it.y == 0f })
            commands.clear()
        }
        pad.performTouchInput { moveTo(center); up() }
        compose.runOnIdle { assertTrue(commands.isEmpty()) }
        pad.performTouchInput {
            val newAnchor = center + Offset(50 * density, 0f)
            down(newAnchor)
            moveTo(newAnchor + Offset(0f, -30 * density))
            up()
        }
        compose.runOnIdle {
            assertTrue(commands.isNotEmpty())
            assertTrue(commands.all { it.x == 0f && it.y < 0 })
        }
    }
}
