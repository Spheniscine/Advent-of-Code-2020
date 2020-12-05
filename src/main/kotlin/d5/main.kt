package d5

import commons.*
import java.io.File

private val input by lazyInput(5, "gmail")

fun main() {
    println("--- Day 5: Binary Boarding ---")
    markTime()

    val passes = input.lines()

    val nums = IntArray(passes.size) { i ->
        val pass = passes[i]

        pass.translate("FBLR", "0101").toInt(2)
    }
    nums.sort()

    val ans1 = nums.last()

    println("Part 1: $ans1")
    printTime()

    markTime()

    val idx = (0 until nums.lastIndex).first { nums[it+1] - nums[it] != 1 }
    val ans2 = nums[idx] + 1

    println("Part 2: $ans2")
    printTime()
}


