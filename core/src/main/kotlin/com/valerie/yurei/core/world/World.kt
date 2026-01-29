package com.valerie.yurei.core.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Représente une âme lumineuse collectible dans le monde.
 */
data class Soul(
    val id: Int,
    val position: Offset,
    val radius: Float = 20f,
    val lightIntensity: Float = 1f,
    val pulsePhase: Float = 0f // Phase pour l'animation de pulsation
) {
    /**
     * Retourne le rayon actuel avec effet de pulsation.
     */
    fun getCurrentRadius(timeMs: Long): Float {
        val pulse = kotlin.math.sin(pulsePhase + timeMs * 0.003f) * 0.2f + 1f
        return radius * pulse
    }
}

/**
 * Représente une particule de brume pour l'ambiance.
 */
data class FogParticle(
    val position: Offset,
    val size: Float,
    val opacity: Float,
    val speed: Float,
    val direction: Float // Angle en radians
) {
    fun update(dtMs: Long, worldSize: Size): FogParticle {
        val dx = kotlin.math.cos(direction) * speed * dtMs * 0.01f
        val dy = kotlin.math.sin(direction) * speed * dtMs * 0.01f
        var newX = position.x + dx
        var newY = position.y + dy

        // Réapparition de l'autre côté si hors limites
        if (newX < 0) newX += worldSize.width
        if (newX > worldSize.width) newX -= worldSize.width
        if (newY < 0) newY += worldSize.height
        if (newY > worldSize.height) newY -= worldSize.height

        return copy(position = Offset(newX, newY))
    }
}

/**
 * Gère le monde de jeu : âmes, brume, collisions.
 */
class World(
    worldSize: Size,
    initialSoulCount: Int = 5
) {
    private var size: Size = worldSize
    private val souls = mutableListOf<Soul>()
    private val fogParticles = mutableListOf<FogParticle>()
    private var nextSoulId = 0
    private var timeMs: Long = 0L

    // Paramètres de génération
    private val minSoulCount = 3
    private val maxSoulCount = 8
    private val soulSpawnRadius = 50f // Distance minimale du centre pour spawner

    init {
        generateSouls(initialSoulCount)
        generateFog(30) // 30 particules de brume
    }

    /**
     * Met à jour le monde (animations, régénération d'âmes).
     */
    fun update(dtMs: Long) {
        timeMs += dtMs

        // Régénérer les âmes si nécessaire
        if (souls.size < minSoulCount) {
            val toSpawn = Random.nextInt(minSoulCount - souls.size, maxSoulCount - souls.size + 1)
            generateSouls(toSpawn)
        }

        // Mettre à jour les particules de brume
        fogParticles.replaceAll { particle ->
            particle.update(dtMs, size)
        }
    }

    /**
     * Génère des âmes lumineuses aléatoirement dans le monde.
     */
    private fun generateSouls(count: Int) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        repeat(count) {
            // Générer une position aléatoire, mais éviter le centre
            val angle = Random.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val distance = Random.nextFloat() * (size.width.coerceAtMost(size.height) / 3f) + soulSpawnRadius
            val x = centerX + kotlin.math.cos(angle) * distance
            val y = centerY + kotlin.math.sin(angle) * distance

            // S'assurer que l'âme est dans les limites
            val clampedX = x.coerceIn(30f, size.width - 30f)
            val clampedY = y.coerceIn(30f, size.height - 30f)

            souls.add(
                Soul(
                    id = nextSoulId++,
                    position = Offset(clampedX, clampedY),
                    radius = Random.nextFloat() * 10f + 15f, // Entre 15 et 25
                    lightIntensity = Random.nextFloat() * 0.3f + 0.7f, // Entre 0.7 et 1.0
                    pulsePhase = Random.nextFloat() * kotlin.math.PI.toFloat() * 2f
                )
            )
        }
    }

    /**
     * Génère des particules de brume pour l'ambiance.
     */
    private fun generateFog(count: Int) {
        repeat(count) {
            fogParticles.add(
                FogParticle(
                    position = Offset(
                        Random.nextFloat() * size.width,
                        Random.nextFloat() * size.height
                    ),
                    size = Random.nextFloat() * 80f + 40f, // Entre 40 et 120
                    opacity = Random.nextFloat() * 0.3f + 0.1f, // Entre 0.1 et 0.4
                    speed = Random.nextFloat() * 0.5f + 0.1f, // Entre 0.1 et 0.6
                    direction = Random.nextFloat() * kotlin.math.PI.toFloat() * 2f
                )
            )
        }
    }

    /**
     * Vérifie les collisions entre le dragon et les âmes.
     * Retourne la liste des IDs des âmes collectées.
     */
    fun checkSoulCollisions(
        dragonHeadPosition: Offset,
        dragonHeadRadius: Float
    ): List<Int> {
        val collectedIds = mutableListOf<Int>()
        val iterator = souls.iterator()

        while (iterator.hasNext()) {
            val soul = iterator.next()
            val dx = dragonHeadPosition.x - soul.position.x
            val dy = dragonHeadPosition.y - soul.position.y
            val distance = sqrt(dx * dx + dy * dy)
            val currentRadius = soul.getCurrentRadius(timeMs)

            if (distance < (dragonHeadRadius + currentRadius)) {
                collectedIds.add(soul.id)
                iterator.remove()
            }
        }

        return collectedIds
    }

    /**
     * Retourne la liste actuelle des âmes.
     */
    fun getSouls(): List<Soul> = souls.toList()

    /**
     * Retourne la liste actuelle des particules de brume.
     */
    fun getFogParticles(): List<FogParticle> = fogParticles.toList()

    /**
     * Retourne le temps écoulé en millisecondes.
     */
    fun getTimeMs(): Long = timeMs

    /**
     * Met à jour la taille du monde (utile lors des changements d'orientation).
     */
    fun updateSize(newSize: Size) {
        size = newSize
    }

    /**
     * Réinitialise le monde.
     */
    fun reset() {
        souls.clear()
        fogParticles.clear()
        nextSoulId = 0
        timeMs = 0L
        generateSouls(minSoulCount + Random.nextInt(maxSoulCount - minSoulCount + 1))
        generateFog(30)
    }
}
