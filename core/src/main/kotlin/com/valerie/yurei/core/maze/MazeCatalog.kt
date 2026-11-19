package com.valerie.yurei.core.maze

/**
 * Liste des tableaux du jeu. Pour en ajouter un : écrire son plan ici et l'ajouter
 * à [all] ; les tests vérifient automatiquement que toutes ses âmes sont atteignables
 * et qu'il n'a aucun cul-de-sac (voir [MazePlan.deadEnds]).
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

    /**
     * Lac des lanternes, tableau 2 : 20 âmes, 5 cases/s. Plus de carrefours ; les âmes
     * du centre (colonne 4) se trouvent dans des passages étroits entre deux rochers.
     */
    val lake2 = MazeLevel(
        id = "lac-2",
        cellsPerSecond = 5f,
        startDirection = Direction.Right,
        plan = MazePlan.parse(
            """
            D..o.o..o
            .##.#.##.
            o#..o..#o
            .#.###.#.
            ..o...o..
            ##.#.#.##
            o..#o#..o
            .#.....#.
            .#.#.#.#.
            o.o#.#o.o
            .#..o..#.
            .#.#.#.#.
            ...#o#...
            .#.....#.
            .##.#.##.
            o...o...o
            """
        )
    )

    /**
     * Lac des lanternes, tableau 3 : 25 âmes, 6 cases/s. Un damier de rochers :
     * des carrefours partout, il faut choisir son chemin pour ne pas se couper la route.
     */
    val lake3 = MazeLevel(
        id = "lac-3",
        cellsPerSecond = 6f,
        startDirection = Direction.Right,
        plan = MazePlan.parse(
            """
            D.o.o.o.o
            .#.#.#.#.
            o.o#o#o.o
            ##.....##
            o..#o#..o
            .#.#.#.#.
            .o.....o.
            .###.###.
            o.......o
            .#.#o#.#.
            .#o#.#o#.
            ...#.#...
            o#.....#o
            .#.#o#.#.
            .#.#.#.#.
            o...o...o
            """
        )
    )

    /** Les trois tableaux du lac, dans l'ordre : le souvenir arrive après le dernier. */
    val lake: List<MazeLevel> = listOf(lake1, lake2, lake3)

    val all: List<MazeLevel> = lake

    fun byId(id: String): MazeLevel? = all.firstOrNull { it.id == id }
}
