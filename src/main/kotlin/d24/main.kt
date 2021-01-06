package d24

import commons.*
import java.io.File

private val input by lazyInput(24, "gmail")

fun main() {
    println("--- Day 24: Lobby Layout ---")
    markTime()

    val lines = input.lines()
    var isBlack = HashSet<Vec2>()

    for(line in lines) {
        var pos = Vec2.ORIGIN
        var str = ""

        for(char in line) {
            str += char
            hexDirMap[str]?.let { dir ->
                pos += dir
                str = ""
            }
        }

        if(!isBlack.add(pos)) isBlack.remove(pos)
    }

    val ans1 = isBlack.size

    println("Part 1: $ans1")
    printTime()

    markTime()

    repeat(100) {
        val adj = HashMap<Vec2, Int>().default(0)

        for(pos in isBlack) {
            for(dir in HexDir.values) {
                adj[pos + dir]++
            }
        }

        isBlack = adj.keys.filterTo(HashSet()) { pos ->
            val num = adj[pos]
            num == 2 || num == 1 && pos in isBlack
        }
    }

    val ans2 = isBlack.size

    println("Part 2: $ans2")
    printTime()
}

enum class HexDir(val vec: Vec2) {
    East(Vec2(1, 0)),
    Southeast(Vec2(0, 1)),
    Southwest(Vec2(-1, 1)),
    West(Vec2(-1, 0)),
    Northwest(Vec2(0, -1)),
    Northeast(Vec2(1, -1));

    companion object {
        val values = values().asList()
    }
}

operator fun Vec2.plus(dir: HexDir) = plus(dir.vec)

val hexDirMap by lazy { mapOf(
    "e" to HexDir.East,
    "se" to HexDir.Southeast,
    "sw" to HexDir.Southwest,
    "w" to HexDir.West,
    "nw" to HexDir.Northwest,
    "ne" to HexDir.Northeast
) }