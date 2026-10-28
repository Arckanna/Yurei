package com.valerie.yurei.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class SavedPoint(val x: Float, val y: Float)
@Serializable data class SavedSoul(val id: Int, val position: SavedPoint, val radius: Float, val light: Float, val pulse: Float)
@Serializable data class SavedFog(val position: SavedPoint, val size: Float, val opacity: Float, val speed: Float, val direction: Float)
@Serializable data class SavedGame(
    val version: Int = 2,
    val level: Int = 1,
    val remainingMs: Long = 180_000,
    val collected: Int = 0,
    val graceMs: Long = 0,
    // null means the cause is unknown (saves made before it was recorded).
    val hitScenery: Boolean? = null,
    val heading: Float = -1.5707964f,
    val desiredHeading: Float = heading,
    val width: Float,
    val height: Float,
    val segments: List<SavedPoint>,
    val deployedSegments: List<Boolean>? = null,
    val light: Float,
    val score: Int,
    val souls: List<SavedSoul>,
    val fog: List<SavedFog>,
    val worldTimeMs: Long,
    val idleMs: Long,
    val erosionMs: Long,
    val chapter: Int = 0,
    val chapterMs: Long = 0,
    val offerings: Int = 0,
    val journeyMs: Long = 0,
    val completed: Boolean = false,
    val journeyStarted: Boolean = true
) {
    fun isValid(): Boolean {
        fun point(p: SavedPoint) = p.x.isFinite() && p.y.isFinite() && p.x in 0f..width && p.y in 0f..height
        val target = if (version >= 4) 30 else 20
        return version in 1..7 && level in 1..3 && (level < 3 || version >= 7) && (version >= 5 || level == 1) && remainingMs in 1..180_000 && collected in 0..target && graceMs in 0..2000 && heading.isFinite() && desiredHeading.isFinite() && (version < 3 || completed == (collected == target)) && chapter in 0..2 && chapterMs in 0..100_000L &&
            offerings in 0..4 && journeyMs >= 0 &&
            (!completed || version >= 3 && collected == target && journeyStarted || version < 3 && chapter == 2 && (chapterMs >= 60_000L && offerings >= 4 || chapterMs >= 100_000L && offerings >= 1) && journeyMs >= 180_000L && journeyStarted) && width.isFinite() && height.isFinite() && width > 0 && height > 0 &&
            (deployedSegments == null && version < 6 || deployedSegments?.size == segments.size) &&
            segments.isNotEmpty() && segments.all(::point) && light.isFinite() && light > 0f && light <= 1f &&
            score >= 0 && worldTimeMs >= 0 && idleMs in 0..8_000L && erosionMs in 0..7_999L &&
            souls.size <= 8 && souls.map { it.id }.distinct().size == souls.size &&
            souls.all { it.id in 0 until Int.MAX_VALUE && point(it.position) && it.radius.isFinite() && it.radius in 1f..30f && it.light.isFinite() && it.light in 0f..1f && it.pulse.isFinite() } &&
            fog.size <= 30 && fog.all { point(it.position) && it.size.isFinite() && it.size in 1f..200f && it.opacity.isFinite() && it.opacity in 0f..1f && it.speed.isFinite() && it.speed in 0f..1f && it.direction.isFinite() }
    }
}

object SavedGameCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(game: SavedGame): String {
        require(game.isValid()) { "Invalid save" }
        return json.encodeToString(SavedGame.serializer(), game)
    }
    fun decode(value: String): SavedGame = json.decodeFromString(SavedGame.serializer(), value).also {
        require(it.isValid()) { "Invalid or unsupported save" }
    }
}
