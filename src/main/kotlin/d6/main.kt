package d6

import commons.*
import java.io.File

private val input by lazyInput(6, "gmail")

fun main() {
    println("--- Day 6: Custom Customs ---")
    markTime()

    val groupStrings = input.split("\n\n")

    var ans1 = 0

    for(s in groupStrings) {
        val present = BooleanArray(26)
        for(c in s) {
            if(c in 'a'..'z') present[c - 'a'] = true
        }
        ans1 += present.count { it }
    }

    println("Part 1: $ans1")
    printTime()

    markTime()

    val groups = groupStrings.map { it.lines() }

    var ans2 = 0

    for(g in groups) {
        val cnt = IntArray(26)
        for(s in g) for(c in s) {
            cnt[c - 'a']++
        }
        val m = g.size
        ans2 += cnt.count { it == m }
    }

    println("Part 2: $ans2")
    printTime()
}