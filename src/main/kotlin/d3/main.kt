package d3

import commons.*
import java.io.File

private val input by lazyInput(3, "gmail")

fun main() {
    println("--- Day 3: Toboggan Trajectory ---")
    markTime()

    val G = input.lines()
    val width = G[0].length
    val height = G.size

    fun f(right: Int, down: Int): Int {
        var ans = 0

        var j = 0
        for(i in down until height step down) {
            j = (j + right) % width
            if(G[i][j] == '#') ans++
        }

        return ans
    }

    val ans1 = f(3, 1)

    println("Part 1: $ans1")
    printTime()

    markTime()

    val ans2 = f(1, 1).toLong() * ans1 * f(5, 1) * f(7, 1) * f(1, 2)

    println("Part 2: $ans2")
    printTime()
}