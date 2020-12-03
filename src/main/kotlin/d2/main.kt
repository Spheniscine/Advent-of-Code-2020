package d2

import commons.*
import java.io.File

private val input by lazyInput(2, "gmail")

fun main() {
    println("--- Day 2: Password Philosophy ---")
    markTime()

    val regex = Regex("""(\d+)-(\d+) (.): (.+)""")

    val A = input.lines().map {
        val (lo, hi, char, pass) = regex.capture(it)!!
        Entry(lo.toInt(), hi.toInt(), char[0], pass)
    }
    val ans1 = A.count { it.isOk() }

    println("Part 1: $ans1")
    printTime()

    markTime()
    val ans2 = A.count { it.isOk2() }

    println("Part 2: $ans2")
    printTime()
}

data class Entry(val lo: Int, val hi: Int, val char: Char, val pass: String) {
    fun isOk() = pass.count { it == char } in lo..hi
    fun isOk2() = (pass[lo-1] == char) xor (pass[hi-1] == char)
}

