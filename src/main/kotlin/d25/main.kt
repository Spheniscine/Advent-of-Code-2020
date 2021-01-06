package d25

import commons.*
import java.io.File

private val input by lazyInput(25, "gmail")

fun main() {
    println("--- Day 25: Combo Breaker ---")

    markTime()

    val (a, b) = input.lines().map { it.toInt() }
    val y = discreteLog(b)

    val ans1 = a.powMod(y, m)

    println("Part 1: $ans1")
    printTime()
}

fun discreteLog(a: Int): Int {
    var x = 0
    var v = 1

    while(v != a) {
        v = v.mulMod(g, m)
        x++
    }

    return x
}

const val g = 7
const val m = 20201227