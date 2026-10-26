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
    private val onUpdate: (simulationMs: Long, activeMs: Long) -> Unit
) {
    init { require(tickMs > 0) }

    // Start/stop and updates run on the owning scope's thread (Main in the app).
    private var job: Job? = null
    private var lastTickMs = 0L

    fun start() {
        if (job?.isActive == true) return
        lastTickMs = clockMs()
        job = scope.launch(start = CoroutineStart.LAZY) {
            while (isActive) {
                delay(tickMs)
                // Keep the full monotonic duration for deadlines, but bound physical motion.
                val activeMs = consumePendingTime()
                val dt = activeMs.coerceAtMost(50L)
                if (dt > 0) onUpdate(dt, activeMs)
            }
        }
        job?.start()
    }
    /** Settle the final fraction before pausing, without advancing physical simulation. */
    fun consumePendingTime(): Long {
        if (job?.isActive != true) return 0L
        val now = clockMs()
        val elapsed = (now - lastTickMs).coerceAtLeast(0L)
        lastTickMs = now
        return elapsed
    }
    fun stop() {
        job?.cancel()
        job = null
    }
}
