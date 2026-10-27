package com.valerie.yurei.core.entity

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import kotlin.math.pow
import androidx.compose.ui.geometry.Size
import com.valerie.yurei.core.world.confinedTo
import com.valerie.yurei.core.world.rescaled

/**
 * Représente un segment du corps du dragon spectral.
 * Chaque segment suit le précédent pour créer un effet de serpent.
 */
data class DragonSegment(
    val position: Offset,
    val light: Float = 0.5f, // Intensité lumineuse du segment (0f à 1f)
    val deployed: Boolean = false // Link to the preceding segment has unfolded.
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
    companion object {
        // Logical world units: collection reach must not depend on the remaining light.
        const val HEAD_RADIUS = 48f
        // Visible head ellipse, shared by drawing and physical collisions.
        // The glow, horns and whiskers are decorative; collection keeps its generous reach.
        const val HEAD_HALF_WIDTH = 28.8f
        const val HEAD_HALF_HEIGHT = HEAD_HALF_WIDTH * 0.65f
    }
    // Position de la tête (premier segment)
    var headPosition: Offset = initialPosition
        private set

    // Intensité lumineuse globale (affecte tous les segments)
    var light: Float = initialLight.coerceIn(0f, 1f)
        private set

    // Longueur du corps (nombre de segments)
    var length: Int = initialLength.coerceAtLeast(1)
        private set

    // Liste des segments du corps (tête = index 0)
    private val segments = mutableListOf<DragonSegment>()

    // Distance minimale entre segments pour créer l'effet de suivi
    private val segmentSpacing: Float = 30f

    // Vitesse de suivi des segments (0f à 1f, plus élevé = suivi plus rapide)
    private val followSpeed: Float = 0.15f

    init {
        // Créer immédiatement tous les segments demandés, tête comprise.
        repeat(length) { segments.add(DragonSegment(initialPosition, light)) }
    }

    /**
     * Déplace la tête ; le corps suit lors des mises à jour temporelles.
     */
    fun updateHeadPosition(newPosition: Offset) {
        if (!newPosition.x.isFinite() || !newPosition.y.isFinite()) return
        headPosition = newPosition
        segments[0] = DragonSegment(headPosition, light)
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
        segments.add(lastSegment.copy(deployed = false))
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
    fun update(dtMs: Long) {
        if (dtMs <= 0) return
        val followFactor = 1f - (1f - followSpeed).pow(dtMs / 16f)
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
                val moveDistance = (distance - segmentSpacing) * followFactor
                val angle = kotlin.math.atan2(dy, dx)
                val newX = current.x + kotlin.math.cos(angle) * moveDistance
                val newY = current.y + kotlin.math.sin(angle) * moveDistance

                val segmentLight = light * (1f - i * 0.1f).coerceIn(0.3f, 1f)
                segments[i] = segments[i].copy(position = Offset(newX, newY), light = segmentLight)
            } else {
                // Le segment est assez proche, juste mettre à jour la lumière
                val segmentLight = light * (1f - i * 0.1f).coerceIn(0.3f, 1f)
                segments[i] = segments[i].copy(light = segmentLight)
            }
        }

        // Deployment is permanent: compressing an established link must not disable it.
        for (i in 1 until segments.size) {
            if (!segments[i].deployed &&
                (segments[i].position - segments[i - 1].position).getDistance() >= segmentSpacing) {
                segments[i] = segments[i].copy(deployed = true)
            }
        }

        // Synchroniser la longueur avec le nombre de segments
        while (segments.size > length) {
            segments.removeLastOrNull()
        }
        while (segments.size < length) {
            val lastSegment = segments.lastOrNull() ?: DragonSegment(headPosition, light)
            segments.add(lastSegment.copy(deployed = false))
        }
    }

    /**
     * Retourne la liste des segments pour le rendu.
     */
    fun getSegments(): List<DragonSegment> = segments.toList()

    fun restore(positions: List<Offset>, restoredLight: Float, deployed: List<Boolean>? = null) {
        require(positions.isNotEmpty())
        require(deployed == null || deployed.size == positions.size)
        segments.clear()
        positions.forEachIndexed { i, position ->
            // Older saves infer deployment from their geometry.
            val ready = deployed?.get(i) ?: (i > 0 &&
                (position - positions[i - 1]).getDistance() >= segmentSpacing)
            segments.add(DragonSegment(position, restoredLight, ready))
        }
        length = positions.size
        headPosition = positions.first()
        setLight(restoredLight)
    }

    fun moveBy(dx: Float, dy: Float, size: Size) {
        if (!dx.isFinite() || !dy.isFinite()) return
        updateHeadPosition((headPosition + Offset(dx, dy)).confinedTo(size, 48f))
    }

    fun resize(from: Size, to: Size) {
        segments.replaceAll { it.copy(position = it.position.rescaled(from, to).confinedTo(to, 48f)) }
        headPosition = segments.first().position
    }

    /**
     * Retourne le rayon de collision de la tête.
     */
    fun getHeadRadius(): Float = HEAD_RADIUS

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
