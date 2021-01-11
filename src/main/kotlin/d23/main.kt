package d23

import commons.*
import java.io.File

private val input by lazyInput(23, "gmail")

fun main() {
    println("--- Day 23: Crab Cups ---")
    markTime()

    run {
        val in_ = IntArray(input.length) { input[it] - '0' }

        CrabCup(input.length, in_[0]).run {
            for (i in 0 until n) {
                val a = in_[i]
                val b = in_.getOrElse(i + 1) { in_[0] }

                nx[a] = b
            }

            simulate(100)

            curr = 1

            val ans1 = String(CharArray(n - 1) {
                curr = nx[curr]
                '0' + curr
            })

            println("Part 1: $ans1")
            printTime()
        }
    }

    markTime()


    val m = input.length
    val in_ = IntArray(m) { input[it] - '0' }

    CrabCup(1e6.toInt(), in_[0]).run {
        for (i in 0 until m) {
            val a = in_[i]
            val b = in_.getOrElse(i+1) { m+1 }

            nx[a] = b
        }
        for(i in m+1 .. n) {
            val b = if(i < n) i + 1 else in_[0]

            nx[i] = b
        }

        simulate(1e7.toInt())

        val ans2 = nx[1].toLong() * nx[nx[1]]

        println("Part 2: $ans2")
        printTime()
    }
}

class CrabCup(val n: Int, var curr: Int) {
    val nx = IntArray(n + 1)

    fun simulate(rounds: Int) {
        repeat(rounds) {
            val pick = IntArray(3)
            pick[0] = nx[curr]
            pick[1] = nx[pick[0]]
            pick[2] = nx[pick[1]]

            var next = nx[pick[2]]
            nx[curr] = next

            var dest = curr - 1
            while (true) {
                if (dest == 0) dest = n
                if (dest !in pick) break
                dest--
            }

            next = nx[dest]
            nx[dest] = pick[0]
            nx[pick[2]] = next

            curr = nx[curr]
        }
    }
}