package d23

import commons.*
import java.io.File

private val input by lazyInput(23, "gmail")

fun main() {
    println("--- Day 23: Crab Cups ---")
    markTime()

    run {
        val n = input.length
        val in_ = IntArray(n) { input[it] - '0' }
        val pr = IntArray(n + 1)
        val nx = IntArray(n + 1)

        for (i in 0 until n) {
            val a = in_[i]
            val b = in_.getOrElse(i + 1) { in_[0] }

            nx[a] = b
            pr[b] = a
        }

        var curr = in_[0]

        repeat(100) {
            val pick = IntArray(3)
            pick[0] = nx[curr]
            pick[1] = nx[pick[0]]
            pick[2] = nx[pick[1]]

            var next = nx[pick[2]]
            nx[curr] = next
            pr[next] = curr

            var dest = curr - 1
            while (true) {
                if (dest == 0) dest = n
                if (dest !in pick) break
                dest--
            }

            next = nx[dest]
            nx[dest] = pick[0]
            pr[pick[0]] = dest
            nx[pick[2]] = next
            pr[next] = pick[2]

            curr = nx[curr]
        }

        curr = 1

        val ans1 = String(CharArray(n - 1) {
            curr = nx[curr]
            '0' + curr
        })

        println("Part 1: $ans1")
        printTime()
    }

    markTime()

    val n = 1000000
    val m = input.length
    val in_ = IntArray(m) { input[it] - '0' }
    val pr = IntArray(n + 1)
    val nx = IntArray(n + 1)

    for (i in 0 until m) {
        val a = in_[i]
        val b = in_.getOrElse(i+1) { m+1 }

        nx[a] = b
        pr[b] = a
    }
    for(i in m+1 .. n) {
        val b = if(i < n) i + 1 else in_[0]

        nx[i] = b
        pr[b] = i
    }

    var curr = in_[0]

    repeat(10000000) {
        val pick = IntArray(3)
        pick[0] = nx[curr]
        pick[1] = nx[pick[0]]
        pick[2] = nx[pick[1]]

        var next = nx[pick[2]]
        nx[curr] = next
        pr[next] = curr

        var dest = curr - 1
        while (true) {
            if (dest == 0) dest = n
            if (dest !in pick) break
            dest--
        }

        next = nx[dest]
        nx[dest] = pick[0]
        pr[pick[0]] = dest
        nx[pick[2]] = next
        pr[next] = pick[2]

        curr = nx[curr]
    }

    curr = 1

    val ans2 = nx[curr].toLong() * nx[nx[curr]]

    println("Part 2: $ans2")
    printTime()
}