package com.valerie.yurei.core.maze

/**
 * Traduit un glissement du doigt en direction (cf. REGLES_LABYRINTHE.md §3.3).
 *
 * [dx] et [dy] sont le déplacement du doigt à l'écran (y vers le bas).
 * Le glissement ne compte qu'à partir de [minDistance], pour qu'un simple
 * contact ou un tremblement ne fasse pas tourner le dragon. L'axe le plus
 * marqué l'emporte ; à égalité, l'horizontal est choisi.
 *
 * @return la direction, ou null si le doigt n'a pas encore assez bougé.
 */
fun swipeDirection(dx: Float, dy: Float, minDistance: Float): Direction? {
    require(minDistance > 0f) { "Distance minimale invalide : $minDistance" }
    val absX = kotlin.math.abs(dx)
    val absY = kotlin.math.abs(dy)
    if (maxOf(absX, absY) < minDistance) return null
    return if (absX >= absY) {
        if (dx > 0) Direction.Right else Direction.Left
    } else {
        if (dy > 0) Direction.Down else Direction.Up
    }
}
