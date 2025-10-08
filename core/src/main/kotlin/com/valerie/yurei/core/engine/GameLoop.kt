package com.valerie.yurei.core.engine


import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class GameLoop(
    private val scope: CoroutineScope,
    private val tickMs: Long = 16L, // ~60fps
    private val onUpdate: (dtMs: Long) -> Unit
) {
    private var running = false
    fun start() {
        if (running) return
        running = true
        scope.launch {
            var last = System.currentTimeMillis()
            while (isActive && running) {
                val now = System.currentTimeMillis()
                val dt = now - last
                last = now
                onUpdate(dt)
                delay(tickMs)
            }
        }
    }
    fun stop() { running = false }
}