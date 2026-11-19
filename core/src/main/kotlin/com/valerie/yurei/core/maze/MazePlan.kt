package com.valerie.yurei.core.maze

/** Une case de la grille : colonne et ligne, (0, 0) étant en haut à gauche. */
data class Cell(val col: Int, val row: Int) {
    fun next(direction: Direction) = Cell(col + direction.dCol, row + direction.dRow)
}

/** Les quatre directions possibles sur la grille. */
enum class Direction(val dCol: Int, val dRow: Int) {
    Up(0, -1), Down(0, 1), Left(-1, 0), Right(1, 0);

    val opposite: Direction
        get() = when (this) {
            Up -> Down
            Down -> Up
            Left -> Right
            Right -> Left
        }
}

/**
 * Plan d'un tableau, écrit en texte : une ligne de texte = une ligne de cases.
 *
 * - `#` : mur
 * - `.` : couloir vide
 * - `o` : couloir avec une âme
 * - `D` : case de départ du dragon (un couloir)
 *
 * Le bord de la grille compte comme un mur : inutile d'entourer le plan de `#`.
 * Le plan est vérifié à la lecture, pour qu'une faute de frappe soit signalée
 * tout de suite plutôt que pendant une partie.
 */
class MazePlan private constructor(
    val columns: Int,
    val rows: Int,
    private val walls: Set<Cell>,
    val souls: Set<Cell>,
    val start: Cell
) {
    fun isInside(cell: Cell) = cell.col in 0 until columns && cell.row in 0 until rows

    /** Une case où le dragon peut aller : dans la grille et sans mur. */
    fun isOpen(cell: Cell) = isInside(cell) && cell !in walls

    /** Toutes les cases que l'on peut atteindre depuis le départ (parcours en largeur). */
    fun reachableCells(): Set<Cell> {
        val visited = mutableSetOf(start)
        val toVisit = ArrayDeque(listOf(start))
        while (toVisit.isNotEmpty()) {
            val cell = toVisit.removeFirst()
            for (direction in Direction.entries) {
                val neighbour = cell.next(direction)
                if (isOpen(neighbour) && visited.add(neighbour)) toVisit.addLast(neighbour)
            }
        }
        return visited
    }

    /** Âmes impossibles à atteindre : doit être vide pour tout tableau du jeu. */
    fun unreachableSouls(): Set<Cell> = souls - reachableCells()

    /**
     * Culs-de-sac : cases libres avec une seule voisine libre (ou aucune).
     *
     * Doit être vide pour tout tableau du jeu : une fois entré dans un cul-de-sac,
     * un dragon qui a un corps ne peut plus en sortir, car le seul chemin de retour
     * passe par son cou et le demi-tour est ignoré. Il resterait bloqué pour toujours.
     */
    fun deadEnds(): Set<Cell> = reachableCells().filter { cell ->
        Direction.entries.count { isOpen(cell.next(it)) } < 2
    }.toSet()

    companion object {
        const val WALL = '#'
        const val EMPTY = '.'
        const val SOUL = 'o'
        const val START = 'D'

        fun parse(text: String): MazePlan {
            val lines = text.trimIndent().lines().map { it.trimEnd() }.filter { it.isNotEmpty() }
            require(lines.isNotEmpty()) { "Le plan est vide" }
            val columns = lines.first().length
            lines.forEachIndexed { row, line ->
                require(line.length == columns) {
                    "Ligne ${row + 1} : ${line.length} caractères au lieu de $columns"
                }
            }

            val walls = mutableSetOf<Cell>()
            val souls = mutableSetOf<Cell>()
            var start: Cell? = null
            lines.forEachIndexed { row, line ->
                line.forEachIndexed { col, char ->
                    val cell = Cell(col, row)
                    when (char) {
                        WALL -> walls += cell
                        EMPTY -> Unit
                        SOUL -> souls += cell
                        START -> {
                            require(start == null) { "Le plan contient plusieurs départs '$START'" }
                            start = cell
                        }
                        else -> throw IllegalArgumentException(
                            "Caractère inconnu '$char' ligne ${row + 1}, colonne ${col + 1}"
                        )
                    }
                }
            }
            return MazePlan(
                columns = columns,
                rows = lines.size,
                walls = walls,
                souls = souls,
                start = requireNotNull(start) { "Le plan doit contenir un départ '$START'" }
            )
        }
    }
}
