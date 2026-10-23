package com.valerie.yurei.core.engine


import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart


class GameLoop(
    private val scope: CoroutineScope,
    private val tickMs: Long = 16L, // ~60fps
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val onUpdate: (dtMs: Long) -> Unit
) {
    init { require(tickMs > 0) }

    // Start/stop and updates run on the owning scope's thread (Main in the app).
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(start = CoroutineStart.LAZY) {
            var last = clockMs()
            while (isActive) {
                delay(tickMs)
                val now = clockMs()
                // Discard long stalls instead of consuming seconds of light at once.
                val dt = (now - last).coerceIn(0L, 50L)
                last = now
                if (dt > 0) onUpdate(dt)
            }
        }
        job?.start()
    }
    fun stop() {
        job?.cancel()
        job = null
    }
}
