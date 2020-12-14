package d13

import commons.*

private val input by lazyInput(13, "gmail")

fun main() {
    println("--- Day 13 ---")
    markTime()

    val lines = input.lines()

    val minT = lines[0].toLong()
    val buses = lines[1].split(',').map { it.toLongOrNull() }

    val best = buses.filterNotNull().minByOrNull { minT.divCeil(it) * it }!!
    val time = minT.divCeil(best) * best

    val ans1 = best * (time - minT)

    println("Part 1: $ans1")
    printTime()

    markTime()

    var ans2 = 0L
    var lcm = 1L

    for(i in buses.indices) {
        val x = buses[i] ?: continue
        crt(ans2, lcm, -i.toLong(), x) { a, m ->
            ans2 = a
            lcm = m
        }
    }

    println("Part 2: $ans2")
    printTime()
}