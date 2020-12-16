package d15

import commons.*
import kotlin.math.*
import kotlin.random.Random

private val input by lazyInput(15, "gmail")

fun main() {
    println("--- Day 15: Rambunctious Recitation ---")
    markTime()

    val A = input.split(',').map { it.toInt() }
    val mem = Mem()

    for(i in 0 until A.lastIndex) {
        mem[A[i]] = i
    }

    var a = A.last()

    for(i in A.size until 2020) {
        val p = mem[a]
        mem[a] = i-1
        a = if(p == -1) 0 else i-1-p
    }

    val ans1 = a

    println("Part 1: $ans1")
    printTime()

    markTime()

    for(i in 2020 until 30000000) {
        val p = mem[a]
        mem[a] = i-1
        a = if(p == -1) 0 else i-1-p
    }

    val ans2 = a

    println("Part 2: $ans2")
    printTime()
}

class Mem {
    var a = IntArray(12) { -1 }

    operator fun get(i: Int) = if(i >= a.size) -1 else a[i]
    operator fun set(i: Int, v: Int) {
        if(i >= a.size) {
            val s = max(a.size + a.size.shr(1), i+1)
            val new = a.copyOf(s)
            for(j in a.size until s) new[j] = -1

            a = new
        }

        a[i] = v
    }
}