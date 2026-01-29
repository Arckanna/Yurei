package com.valerie.yurei.core.entity

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/**
 * Représente un segment du corps du dragon spectral.
 * Chaque segment suit le précédent pour créer un effet de serpent.
 */
data class DragonSegment(
    val position: Offset,
    val light: Float = 0.5f // Intensité lumineuse du segment (0f à 1f)
)

/**
 * Entité principale du dragon spectral.
 * Gère le corps segmenté qui s'allonge en collectant des âmes.
 */
class Dragon(
    initialPosition: Offset = Offset(0f, 0f),
    initialLength: Int = 1,
    initialLight: Float = 0.5f
) {
    // Position de la tête (premier segment)
    var headPosition: Offset = initialPosition
        private set

    // Intensité lumineuse globale (affecte tous les segments)
    var light: Float = initialLight
        private set

    // Longueur du corps (nombre de segments)
    var length: Int = initialLength
        private set

    // Liste des segments du corps (tête = index 0)
    private val segments = mutableListOf<DragonSegment>()

    // Distance minimale entre segments pour créer l'effet de suivi
    private val segmentSpacing: Float = 30f

    // Vitesse de suivi des segments (0f à 1f, plus élevé = suivi plus rapide)
    private val followSpeed: Float = 0.15f

    init {
        // Initialiser avec un seul segment (la tête)
        segments.add(DragonSegment(initialPosition, initialLight))
    }

    /**
     * Met à jour la position de la tête et fait suivre les segments.
     */
    fun updateHeadPosition(newPosition: Offset) {
        headPosition = newPosition
        updateSegments()
    }

    /**
     * Met à jour l'intensité lumineuse globale.
     */
    fun setLight(newLight: Float) {
        light = newLight.coerceIn(0f, 1f)
        // Mettre à jour la lumière de tous les segments
        segments.forEachIndexed { index, _ ->
            // La lumière diminue progressivement vers la queue
            val segmentLight = light * (1f - index * 0.1f).coerceIn(0.3f, 1f)
            segments[index] = segments[index].copy(light = segmentLight)
        }
    }

    /**
     * Ajoute un segment au corps (quand une âme est collectée).
     */
    fun addSegment() {
        length++
        val lastSegment = segments.lastOrNull() ?: DragonSegment(headPosition, light)
        segments.add(lastSegment.copy())
    }

    /**
     * Retire un segment du corps (quand la lumière diminue trop).
     */
    fun removeSegment() {
        if (length > 1) {
            length--
            segments.removeLastOrNull()
        }
    }

    /**
     * Met à jour les positions des segments pour qu'ils suivent la tête.
     */
    private fun updateSegments() {
        if (segments.isEmpty()) {
            segments.add(DragonSegment(headPosition, light))
            return
        }

        // Mettre à jour la tête (premier segment)
        segments[0] = DragonSegment(headPosition, light)

        // Faire suivre les autres segments
        for (i in 1 until segments.size) {
            val target = segments[i - 1].position
            val current = segments[i].position

            val dx = target.x - current.x
            val dy = target.y - current.y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > segmentSpacing) {
                // Calculer la nouvelle position avec interpolation
                val moveDistance = (distance - segmentSpacing) * followSpeed
                val angle = kotlin.math.atan2(dy, dx)
                val newX = current.x + kotlin.math.cos(angle) * moveDistance
                val newY = current.y + kotlin.math.sin(angle) * moveDistance

                val segmentLight = light * (1f - i * 0.1f).coerceIn(0.3f, 1f)
                segments[i] = DragonSegment(Offset(newX, newY), segmentLight)
            } else {
                // Le segment est assez proche, juste mettre à jour la lumière
                val segmentLight = light * (1f - i * 0.1f).coerceIn(0.3f, 1f)
                segments[i] = segments[i].copy(light = segmentLight)
            }
        }

        // Synchroniser la longueur avec le nombre de segments
        while (segments.size > length) {
            segments.removeLastOrNull()
        }
        while (segments.size < length) {
            val lastSegment = segments.lastOrNull() ?: DragonSegment(headPosition, light)
            segments.add(lastSegment.copy())
        }
    }

    /**
     * Retourne la liste des segments pour le rendu.
     */
    fun getSegments(): List<DragonSegment> = segments.toList()

    /**
     * Retourne le rayon de collision de la tête.
     */
    fun getHeadRadius(): Float = 24f + 24f * light

    /**
     * Vérifie si une position donnée entre en collision avec la tête.
     */
    fun checkCollision(position: Offset, radius: Float): Boolean {
        val dx = headPosition.x - position.x
        val dy = headPosition.y - position.y
        val distance = sqrt(dx * dx + dy * dy)
        return distance < (getHeadRadius() + radius)
    }

    /**
     * Réinitialise le dragon à son état initial.
     */
    fun reset(position: Offset = Offset(0f, 0f), initialLight: Float = 0.5f) {
        headPosition = position
        light = initialLight
        length = 1
        segments.clear()
        segments.add(DragonSegment(position, initialLight))
    }
}
