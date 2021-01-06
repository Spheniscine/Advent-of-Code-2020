package d20

import commons.*
import java.util.EnumMap

private val input by lazyInput(20, "gmail")
private val seaMonsterTxt by lazyFile(20, "seamonster.txt")

fun main() {
    println("--- Day 20: Jurassic Jigsaw ---")
    markTime()

    val tiles = input.trim().split("\n\n").map { str ->
        val lns = str.lines()
        val id = lns[0].removeSurrounding("Tile ", ":").toInt()
        val grid = lns.drop(1)

        Tile(id, grid)
    }

    val byCode = List(rev.size) { mutableListOf<Tile>() }
    for(tile in tiles) for(dir in Dir2.values) {
        byCode[tile.borderCodes[dir]].add(tile)
    }

    for(list in byCode) {
        list.singleOrNull()?.let {
            it.singleCount++
        }
    }

    val corners = tiles.filter { it.singleCount == 2 }

    val ans1 = corners.fold(1L) { acc, tile -> acc * tile.id }

    println("Part 1: $ans1")
    printTime()

    markTime()

    val tilesPerSide = tiles.size.sqrtFloor()
    val finalSideLen = (tileSideLen - 2) * tilesPerSide

    val tilesByPos = Array(tilesPerSide) { Array(tilesPerSide) { corners[0] } }
    val dCodes = Array(tilesPerSide) { IntArray(tilesPerSide) { -1 } }
    val rCodes = Array(tilesPerSide) { IntArray(tilesPerSide) { -1 } }
    val res = Array(finalSideLen) { CharArray(finalSideLen) }

    fun String.isSingleCode() = byCode[unidirCode()].size == 1

    for(i in 0 until tilesPerSide) for(j in 0 until tilesPerSide) {
        val tile: Tile

        if(j == 0) {
            if(i == 0) {
                tile = corners[0]
            } else {
                val code = dCodes[i-1][j].let { minOf(it, rev[it]) }
                val t = tilesByPos[i-1][j]
                tile = byCode[code].first { it != t }
            }
        } else {
            val code = rCodes[i][j-1].let { minOf(it, rev[it]) }
            val t = tilesByPos[i][j-1]
            tile = byCode[code].first { it != t }
        }

        var g = tile.grid
        if(j == 0) {
            if(i == 0) {
                g = g.variants().first {
                    it.row(0).isSingleCode() && it.col(0).isSingleCode()
                }
            } else {
                val code = dCodes[i-1][j]
                g = g.variants().first {
                    it.row(0).code() == code
                }
            }
        } else {
            val code = rCodes[i][j-1]
            g = g.variants().first {
                it.col(0).code() == code
            }
        }

        tilesByPos[i][j] = tile
        dCodes[i][j] = g.row(tileSideLen-1).code()
        rCodes[i][j] = g.col(tileSideLen-1).code()

        for(ii in 0 until tileSideLen - 2) for(jj in 0 until tileSideLen - 2) {
            val iFinal = i * (tileSideLen - 2) + ii
            val jFinal = j * (tileSideLen - 2) + jj

            res[iFinal][jFinal] = g[ii+1][jj+1]
        }
    }

    val seaMonster = mutableListOf<Vec2>()
    run {
        val lines = seaMonsterTxt.lines()
        for(i in lines.indices) for(j in lines[i].indices) {
            if(lines[i][j] == '#') seaMonster.add(Vec2(j, i))
        }
    }

    var seaMonsterCount = 0

    val finalGrid = res.map { String(it) }.variants().first { g ->
        seaMonsterCount = 0

        val n = finalSideLen - seaMonster.maxOf { it.y }
        val m = finalSideLen - seaMonster.maxOf { it.x }
        for(i in 0 until n) for(j in 0 until m) {
            val found = seaMonster.all { (x, y) ->
                g[i+y][j+x] == '#'
            }
            if(found) seaMonsterCount++
        }

        seaMonsterCount > 0
    }

    val ans2 = finalGrid.sumBy { it.count { it == '#' } } - seaMonsterCount * seaMonster.size
    println("Part 2: $ans2")
    printTime()
}

const val tileSideLen = 10
val rev by lazy {
    val n = 1 shl tileSideLen
    val rev = IntArray(n)

    var bit = 1
    var rbit = n shr 1
    while(bit < n) {
        for(i in 0 until bit) {
            rev[i or bit] = rbit or rev[i]
        }
        bit = bit shl 1
        rbit = rbit shr 1
    }

    rev
}

typealias Grid = List<String>
inline val Grid.height get() = size
inline val Grid.width get() = this[0].length

fun Grid.row(i: Int) = this[i]
fun Grid.col(i: Int) = String(CharArray(height) { this[it][i] })

fun Grid.rotateRight() = List(width) { i -> col(height - 1 - i) }
fun Grid.flop() = reversed()

fun Grid.variants() = sequence {
    var g = this@variants
    yield(g)
    repeat(3) {
        g = g.rotateRight()
        yield(g)
    }
    g = flop()
    yield(g)
    repeat(3) {
        g = g.rotateRight()
        yield(g)
    }
}

fun CharSequence.code() = translate(".#", "01").toInt(2)
fun CharSequence.unidirCode() = this.code().let { minOf(it, rev[it]) }

class Tile(val id: Int, val grid: Grid) {

    fun row(i: Int) = grid[i]
    fun col(i: Int) = grid.col(i)

    val borderCodes: NonNullMutableMap<Dir2, Int> = EnumMap<Dir2, Int>(Dir2::class.java).also {
        it[Dir2.Up] = row(0).unidirCode()
        it[Dir2.Right] = col(tileSideLen - 1).unidirCode()
        it[Dir2.Down] = row(tileSideLen - 1).unidirCode()
        it[Dir2.Left] = col(0).unidirCode()
    }.nonNull()

    var singleCount = 0
}