package com.valerie.yurei.audio

import org.junit.Assert.*
import org.junit.Test

class MusicPlaybackStateTest {
    @Test fun savesAndAutomaticTrackChangesCannotResumeInterruptedMusic() {
        val state = MusicPlaybackState()
        state.start(explicitResume = true)
        assertTrue(state.shouldPlay)
        state.interrupt()
        repeat(10) {
            assertFalse(state.setEnabled(true))
            assertFalse(state.shouldPlay)
        }
        state.start(explicitResume = false)
        assertFalse(state.shouldPlay)
        state.pause()
        state.start(explicitResume = true)
        assertTrue(state.shouldPlay)
    }

    @Test fun realSettingChangeCanResumeButCannotOverrideGamePause() {
        val state = MusicPlaybackState()
        state.start(explicitResume = true)
        state.interrupt()
        assertTrue(state.setEnabled(false))
        assertTrue(state.setEnabled(true))
        assertTrue(state.shouldPlay)
        state.pause()
        state.setEnabled(false)
        state.setEnabled(true)
        assertFalse(state.shouldPlay)
    }

    @Test fun explicitResumeStillHonoursDisabledMusic() {
        val state = MusicPlaybackState()
        state.setEnabled(false)
        state.start(explicitResume = true)
        assertFalse(state.shouldPlay)
    }
}
