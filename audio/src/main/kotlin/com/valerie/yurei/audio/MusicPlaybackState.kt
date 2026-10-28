package com.valerie.yurei.audio

/** Playback intent is independent of settings and of a system interruption. */
internal class MusicPlaybackState {
    var enabled = true
        private set
    private var requested = false
    private var interrupted = false
    val shouldPlay get() = enabled && requested && !interrupted

    fun setEnabled(value: Boolean): Boolean {
        if (enabled == value) return false
        enabled = value
        // An actual off/on action is an explicit request to enable music again.
        if (value) interrupted = false
        return true
    }

    fun start(explicitResume: Boolean) {
        requested = true
        if (explicitResume) interrupted = false
    }

    fun pause() { requested = false }
    fun interrupt() { interrupted = true }
}
