package com.valerie.yurei

import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.valerie.yurei.ui.viewmodel.GamePhase
import com.valerie.yurei.ui.viewmodel.GameUiState
import com.valerie.yurei.ui.viewmodel.GameViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EngineLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun snapshot(): GameUiState {
        lateinit var state: GameUiState
        compose.activityRule.scenario.onActivity {
            state = ViewModelProvider(it)[GameViewModel::class.java].state.value
        }
        return state
    }

    @Test fun backgroundFreezesWorldAndRecreationKeepsExplicitResume() {
        // Drive running states on Main: Compose's idle synchronisation otherwise
        // waits for this continuously updating game to end before the next click.
        compose.waitUntil(5_000) { snapshot().ready }
        compose.activityRule.scenario.onActivity {
            ViewModelProvider(it)[GameViewModel::class.java].start()
        }
        compose.waitUntil(5_000) { snapshot().dragon.segments.isNotEmpty() }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val paused = snapshot()
        assertEquals(GamePhase.Paused, paused.phase)
        SystemClock.sleep(250)
        assertEquals(paused, snapshot())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Reprendre").assertExists()
        compose.activityRule.scenario.onActivity {
            val vm = ViewModelProvider(it)[GameViewModel::class.java]
            vm.resume()
            assertEquals(GamePhase.Running, vm.state.value.phase)
            vm.pause()
        }
        compose.onNodeWithText("Quitter").performClick()
        compose.onNodeWithText("Commencer la partie").assertExists()
        compose.onNodeWithText("Continuer la partie").performClick()
        assertEquals(GamePhase.Paused, snapshot().phase)
        compose.onNodeWithText("Reprendre").assertExists()
    }

    @Test fun restartAndBackFromPauseKeepGameUsable() {
        compose.waitUntil(5_000) { snapshot().ready }
        compose.activityRule.scenario.onActivity {
            ViewModelProvider(it)[GameViewModel::class.java].start()
        }
        compose.waitUntil(5_000) { snapshot().dragon.segments.isNotEmpty() }
        compose.activityRule.scenario.onActivity {
            val vm = ViewModelProvider(it)[GameViewModel::class.java]
            repeat(3) {
                vm.pause()
                vm.quit()
                vm.start()
                assertEquals(GamePhase.Running, vm.state.value.phase)
            }
            vm.pause()
        }
        compose.onNodeWithText("Reprendre").assertExists()
        compose.activityRule.scenario.onActivity {
            val vm = ViewModelProvider(it)[GameViewModel::class.java]
            it.onBackPressedDispatcher.onBackPressed()
            assertEquals(GamePhase.Running, vm.state.value.phase)
            vm.pause()
        }
        val paused = snapshot()
        compose.activityRule.scenario.onActivity {
            ViewModelProvider(it)[GameViewModel::class.java].onDrag(1000f, 1000f)
        }
        assertEquals(paused, snapshot())
    }
}
