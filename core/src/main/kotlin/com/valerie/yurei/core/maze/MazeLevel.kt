package com.valerie.yurei.core.maze

/**
 * Un tableau du jeu : son plan et ses réglages.
 *
 * Un tableau est une donnée, pas du code : en ajouter un ne demande pas de toucher
 * aux règles de [MazeRun] (cf. lot C1 du cahier des charges).
 *
 * @param id identifiant stable (ex. "lac-1"), utilisé plus tard par la sauvegarde :
 *           ne jamais le renommer une fois publié.
 * @param cellsPerSecond vitesse du dragon, en cases par seconde.
 * @param startDirection direction dans laquelle le dragon regarde avant le premier glissement.
 */
data class MazeLevel(
    val id: String,
    val plan: MazePlan,
    val cellsPerSecond: Float,
    val startDirection: Direction
) {
    init {
        require(id.isNotBlank()) { "Un tableau doit avoir un identifiant" }
        require(cellsPerSecond > 0f) { "La vitesse doit être positive" }
        require(plan.souls.isNotEmpty()) { "Le tableau $id n'a aucune âme à recueillir" }
    }
}
