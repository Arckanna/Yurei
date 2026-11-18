package com.valerie.yurei.core.maze

/** Un point de la grille en cases, avec décimales : sert au dessin fluide. */
data class GridPoint(val x: Float, val y: Float)

/**
 * Règles d'un tableau en cours (cf. REGLES_LABYRINTHE.md §3).
 *
 * Kotlin pur, sans Android ni Compose : tout se teste sur la JVM.
 *
 * Le dragon avance case par case. Entre deux cases, [stepProgress] (0 à 1) indique
 * où il en est, pour que l'écran puisse le dessiner en mouvement fluide.
 *
 * Le temps ne compte que lorsque le dragon avance. Arrêté (avant le premier
 * glissement, devant un mur, après une morsure), il ne perd ni protection ni bonus.
 * La pause n'a rien de spécial à faire : il suffit de ne plus appeler [update].
 *
 * @param startLanterns lanternes au départ (3 en jeu ; modifiable pour les tests
 *                      et, plus tard, pour la reprise d'une sauvegarde).
 */
class MazeRun(
    val level: MazeLevel,
    startLanterns: Int = START_LANTERNS
) {
    companion object {
        const val START_LANTERNS = 3
        const val GRACE_MS = 2_000L
        const val SOUL_POINTS = 10
        const val LEVEL_POINTS = 500
        const val BONUS_LIMIT_MS = 90_000L
        const val BONUS_POINTS_PER_SECOND = 10

        /** 10 points par seconde restante sous 90 s, jamais négatif. */
        fun timeBonus(elapsedMs: Long): Int =
            ((BONUS_LIMIT_MS - elapsedMs).coerceAtLeast(0L) / 1000L).toInt() * BONUS_POINTS_PER_SECOND
    }

    private val plan = level.plan
    private val msPerCell = 1000f / level.cellsPerSecond

    // Corps du dragon, la tête en premier.
    private val segments = ArrayDeque(listOf(plan.start))
    private val souls = plan.souls.toMutableSet()
    private var stepAccumulatorMs = 0f

    val body: List<Cell> get() = segments.toList()
    val head: Cell get() = segments.first()
    val remainingSouls: Set<Cell> get() = souls.toSet()

    /** Direction actuelle ; tant que le dragon avance, la case devant lui est libre. */
    var heading: Direction = level.startDirection
        private set

    /** Dernier glissement, appliqué dès qu'une case le permet. */
    var queued: Direction? = null
        private set

    var moving = false
        private set
    var lanterns = startLanterns
        private set

    /** Protection après une morsure : le dragon peut traverser son propre corps. */
    var graceMs = 0L
        private set

    /** Temps passé à avancer : sert au bonus. */
    var elapsedMs = 0L
        private set
    var score = 0
        private set

    /** Avancement entre la case actuelle et la suivante, de 0 à 1. */
    var stepProgress = 0f
        private set

    val won get() = souls.isEmpty()
    val lost get() = lanterns <= 0
    val finished get() = won || lost
    val translucent get() = graceMs > 0

    init {
        require(startLanterns in 1..START_LANTERNS) { "Lanternes de départ invalides : $startLanterns" }
    }

    /** Glissement du joueur dans une direction. */
    fun swipe(direction: Direction) {
        if (finished) return
        if (moving) {
            // Le demi-tour direct ferait mordre le cou : ignoré dès que le dragon a un corps.
            // Les autres virages sont gardés pour la prochaine case, même si le cou
            // se trouve de ce côté-là à cet instant.
            if (direction == heading.opposite && segments.size > 1) return
            queued = direction
        } else if (canEnter(direction)) {
            heading = direction
            queued = null
            moving = true
            stepAccumulatorMs = 0f
        }
        // Arrêté face à un mur : le glissement est sans effet, le dragon attend.
    }

    /**
     * Fait avancer le temps de [dtMs] millisecondes de jeu.
     * La boucle de jeu envoie des pas courts (50 ms au plus).
     */
    fun update(dtMs: Long) {
        if (finished || !moving || dtMs <= 0) return
        elapsedMs += dtMs
        graceMs = (graceMs - dtMs).coerceAtLeast(0L)
        stepAccumulatorMs += dtMs
        while (moving && !finished && stepAccumulatorMs >= msPerCell) {
            stepAccumulatorMs -= msPerCell
            step()
        }
        stepProgress = if (moving) (stepAccumulatorMs / msPerCell).coerceIn(0f, 1f) else 0f
    }

    /** Bonus de temps actuel, à afficher pendant la partie. */
    fun currentTimeBonus(): Int = timeBonus(elapsedMs)

    /**
     * Position de chaque segment pour l'affichage, en cases (le centre de la case
     * (2, 3) vaut (2.5, 3.5)), la tête en premier.
     *
     * Pendant un pas, chaque segment glisse vers la case du segment qui le précède,
     * de [stepProgress]. Exceptions : la queue reste en place si le dragon va grandir,
     * et tout le dragon reste immobile si le pas en cours finit par une morsure
     * (il s'arrêtera devant son corps, sans s'y enfoncer puis reculer).
     */
    fun bodyPoints(): List<GridPoint> {
        val advancing = moving && !bitesAt(head.next(heading))
        val growing = head.next(heading) in souls
        val t = if (advancing) stepProgress else 0f
        return segments.mapIndexed { i, cell ->
            val target = when {
                i == 0 -> cell.next(heading)
                i == segments.lastIndex && growing -> cell
                else -> segments[i - 1]
            }
            GridPoint(
                cell.col + 0.5f + (target.col - cell.col) * t,
                cell.row + 0.5f + (target.row - cell.row) * t
            )
        }
    }

    /** Le dragon entre dans la case suivante. */
    private fun step() {
        val target = head.next(heading)
        if (bitesAt(target)) {
            bite()
            return
        }
        val eats = target in souls
        segments.addFirst(target)
        if (eats) {
            souls.remove(target)
            score += SOUL_POINTS
        } else {
            segments.removeLast()
        }
        if (won) {
            moving = false
            score += LEVEL_POINTS + timeBonus(elapsedMs)
            return
        }
        chooseNextHeading()
    }

    /** À l'arrivée sur une case : virage mémorisé si possible, sinon tout droit, sinon arrêt. */
    private fun chooseNextHeading() {
        val wanted = queued
        if (wanted != null && canEnter(wanted)) {
            heading = wanted
            queued = null
        } else if (!canEnter(heading)) {
            stop()
        }
        // Sinon on continue tout droit, et le virage reste mémorisé pour un prochain carrefour.
    }

    /**
     * Vrai si entrer dans [target] est une morsure. Pendant un pas, le bout de la queue
     * libère sa case, sauf si le dragon grandit : y entrer n'est donc pas une morsure.
     */
    private fun bitesAt(target: Cell): Boolean {
        if (graceMs > 0L || target !in segments) return false
        val tailLeaving = if (target in souls) null else segments.last()
        return target != tailLeaving
    }

    private fun bite() {
        lanterns -= 1
        stop()
        if (!lost) graceMs = GRACE_MS
    }

    private fun stop() {
        moving = false
        queued = null
        stepAccumulatorMs = 0f
    }

    private fun canEnter(direction: Direction): Boolean {
        val target = head.next(direction)
        return plan.isOpen(target) && target != neck()
    }

    /** Le segment juste derrière la tête, ou null si le dragon n'a pas encore de corps. */
    private fun neck(): Cell? = segments.getOrNull(1)
}
