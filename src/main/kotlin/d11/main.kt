package d11

import commons.*
import java.io.File

private val input by lazyInput(11, "gmail")

fun main() {
    println("--- Day 11: Seating System ---")
    markTime()

    val lines = input.lines()

    var old = lines.map { it.toCharArray() }

    while(true) {
        val new = old.map { it.copyOf() }

        var changed = false

        for(i in new.indices) {
            for(j in new[i].indices) {
                val c = new[i][j]
                if(c == '.') continue

                var occupied = 0
                for(pi in i-1..i+1) if(pi in new.indices) for(pj in j-1..j+1) if(pj in new[pi].indices) {
                    if(old[pi][pj] == '#') occupied++
                }

                when {
                    c == 'L' && occupied == 0 -> {
                        changed = true
                        new[i][j] = '#'
                    }
                    c == '#' && occupied > 4 -> {
                        changed = true
                        new[i][j] = 'L'
                    }
                }
            }
        }

        if(!changed) break
        old = new
    }

    val ans1 = old.sumBy { it.count { it == '#' } }

    println("Part 1: $ans1")
    printTime()

    markTime()

    old = lines.map { it.toCharArray() }

    val neighbors = List(old.size) { i ->
        List(old[i].size) j@ { j ->
            if(old[i][j] == '.') return@j emptyList()
            val res = mutableListOf<Vec2>()

            for(di in -1..1) for(dj in -1..1) if(di != 0 || dj != 0) {
                var ci = i
                var cj = j

                while(true) {
                    ci += di
                    cj += dj

                    if(ci !in old.indices || cj !in old[ci].indices) break
                    if(old[ci][cj] == 'L') {
                        res.add(Vec2(ci, cj))
                        break
                    }
                }
            }

            res
        }
    }

    while(true) {
        val new = old.map { it.copyOf() }

        var changed = false

        for(i in new.indices) {
            for(j in new[i].indices) {
                val c = new[i][j]
                if(c == '.') continue

                val occupied = neighbors[i][j].count { (pi, pj) -> old[pi][pj] == '#' }

                when {
                    c == 'L' && occupied == 0 -> {
                        changed = true
                        new[i][j] = '#'
                    }
                    c == '#' && occupied > 4 -> {
                        changed = true
                        new[i][j] = 'L'
                    }
                }
            }
        }

        if(!changed) break
        old = new
    }

    val ans2 = old.sumBy { it.count { it == '#' } }

    println("Part 2: $ans2")
    printTime()
}