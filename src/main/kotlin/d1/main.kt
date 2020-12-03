package d1

import commons.*
import java.io.File

private val input by lazyInput(1, "gmail")

fun main() {
    println("--- Day 1: Report Repair ---")
    markTime()

    val A0 = input.lines().map { it.toInt() }
    val A = A0.toIntArray()
    A.sort()
    val n = A.size

    val ans1 = run {
        var i = 0
        var j = n - 1

        while (true) {
            while (A[i] + A[j] > target) j--
            if (A[i] + A[j] == target) break
            i++
        }

        A[i] * A[j]
    }

    println("Part 1: $ans1")
    printTime()

    markTime()
    val ans2 = run ans@{
        for (i in 0 until n - 1) for (j in i + 1 until n) {
            val k = A.binarySearch(target - A[i] - A[j])
            if (k >= 0 && k != i && k != j) {
                return@ans A[i] * A[j] * A[k]
            }
        }
        -1
    }

    println("Part 2: $ans2")
    printTime()
}

const val target = 2020