package com.valerie.yurei.core.maze

/**
 * Liste des tableaux du jeu. Pour en ajouter un : écrire son plan ici et l'ajouter
 * à [all] ; les tests vérifient automatiquement que toutes ses âmes sont atteignables.
 *
 * Les plans font 9 colonnes × 16 lignes (cases de 120 unités sur le terrain de 1080 × 1920).
 */
object MazeCatalog {

    /** Lac des lanternes, tableau 1 : apprendre le labyrinthe, couloirs simples, 15 âmes. */
    val lake1 = MazeLevel(
        id = "lac-1",
        cellsPerSecond = 4f,
        startDirection = Direction.Right,
        plan = MazePlan.parse(
            """
            D...o...o
            .###.###.
            .#o...o#.
            .#.###.#.
            o.......o
            .##.#.##.
            ...o#o...
            .#.###.#.
            .#.....#.
            o###.###o
            .........
            .#.#.#.#.
            o.......o
            .#.###.#.
            .#.....#.
            o...o...o
            """
        )
    )

    val all: List<MazeLevel> = listOf(lake1)

    fun byId(id: String): MazeLevel? = all.firstOrNull { it.id == id }
}
