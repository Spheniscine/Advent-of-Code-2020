package d15

import commons.*

private val input by lazyInput(15, "gmail")

fun main() {
    println("--- Day 15 ---")
    markTime()

    val A = input.split(',').map { it.toInt() }
    val map = HashMap<Int, ArrayDeque<Int>>().autoPut { ArrayDeque(2) }

    for(i in A.indices) {
        val a = A[i]
        val d = map[a]
        if(d.size > 1) d.removeFirst()
        d.add(i)
    }

    val ans1 = run {
        var a = A.last()
        for(i in A.size until 2020) {
            var d = map[a]
            a = if(d.size > 1) i-1-d.first() else 0
            d = map[a]
            if(d.size > 1) d.removeFirst()
            d.add(i)
        }

        a
    }

    println("Part 1: $ans1")
    printTime()

    markTime()

    val ans2 = run {
        var a = ans1
        for(i in 2020 until 30000000) {
            var d = map[a]
            a = if(d.size > 1) i-1-d.first() else 0
            d = map[a]
            if(d.size > 1) d.removeFirst()
            d.add(i)
        }

        a
    }


    println("Part 2: $ans2")
    printTime()
}