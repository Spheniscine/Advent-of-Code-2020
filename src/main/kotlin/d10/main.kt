package d10

import commons.*
import java.io.File

private val input by lazyInput(10, "gmail")

fun main() {
    println("--- Day 10: Adapter Array ---")
    markTime()

    val lines = input.lines()
    val n = lines.size + 2
    val A = IntArray(n)
    for(i in lines.indices) A[i] = lines[i].toInt()
    A.sort(toIndex = n-1)
    A[n-1] = A[n-2] + 3

    val cnt = IntArray(4)
    for(i in 1 until n) cnt[A[i] - A[i-1]]++

    val ans1 = cnt[1] * cnt[3]

    println("Part 1: $ans1")
    printTime()

    markTime()

    val ways = LongArray(n)
    ways[0] = 1

    var j = 0
    for(i in 1 until n) {
        while(A[i] - A[j] > 3) j++
        for(k in j until i) ways[i] += ways[k]
    }

    val ans2 = ways[n-1]

    println("Part 2: $ans2")
    printTime()
}