package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.engine.LightBalance
import com.valerie.yurei.core.entity.Dragon

/** Three breaths of 60–100 seconds. No progress while paused, no win from idling. */
class LanternJourney(
    var chapter: Int = 0,
    var chapterMs: Long = 0,
    var offerings: Int = 0,
    var elapsedMs: Long = 0,
    var completed: Boolean = false
) {
    companion object {
        const val MIN_CHAPTER_MS = 60_000L
        const val GENTLE_CHAPTER_MS = 100_000L
        const val OFFERINGS = 4
        val titles = listOf("La rive oubliée", "Les voix de l’eau", "La lumière demeure")
        val refuges = listOf(Offset(0.23f, 0.68f), Offset(0.77f, 0.48f), Offset(0.5f, 0.3f))
    }

    fun refuge(size: Size): Offset = refuges[chapter].let { Offset(it.x * size.width, it.y * size.height) }
    fun refugeRadius(size: Size): Float = size.minDimension * 0.14f
    fun isSheltered(position: Offset, size: Size): Boolean =
        (position - refuge(size)).getDistance() <= refugeRadius(size)

    /** Returns true exactly once per chapter transition, including the ending. */
    fun update(dtMs: Long, collected: Int): Boolean {
        if (completed || dtMs <= 0) return false
        elapsedMs += dtMs
        chapterMs = (chapterMs + dtMs).coerceAtMost(GENTLE_CHAPTER_MS)
        offerings = (offerings + collected.coerceAtLeast(0)).coerceAtMost(OFFERINGS)
        val ready = chapterMs >= MIN_CHAPTER_MS && offerings >= OFFERINGS ||
            chapterMs >= GENTLE_CHAPTER_MS && offerings >= 1
        if (!ready) return false
        if (chapter == 2) completed = true else {
            chapter++
            chapterMs = 0
            offerings = 0
        }
        return true
    }

    fun progress(): Float = if (completed) 1f else
        (chapter + (chapterMs.toFloat() / MIN_CHAPTER_MS).coerceAtMost(1f) * 0.75f +
            offerings.toFloat() / OFFERINGS * 0.25f) / 3f

    fun updateLight(dragon: Dragon, balance: LightBalance, size: Size, dtMs: Long) {
        if (isSheltered(dragon.headPosition, size)) {
            balance.idleMs = 0
            balance.erosionMs = 0
            dragon.setLight((dragon.light + dtMs * 0.000025f).coerceAtMost(1f))
        } else {
            balance.update(dragon, dtMs)
            // This first world teaches fragility without erasing the journey.
            dragon.setLight(dragon.light.coerceAtLeast(0.08f))
        }
    }
}
