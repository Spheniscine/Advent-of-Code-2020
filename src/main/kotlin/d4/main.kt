package d4

import commons.*
import java.io.File

private val input by lazyInput(4, "gmail")

fun main() {
    println("--- Day 4: Passport Processing ---")
    markTime()

    val whitespace = Regex("""\s+""")

    val A = input.split("\n\n").map { s ->
        val res = mutableMapOf<String, String>()
        val entries = s.split(whitespace).map { it.split(':') }

        for((k, v) in entries) res[k] = v

        res as Map<String, String>
    }

    val need = listOf("byr", "iyr", "eyr", "hgt", "hcl", "ecl", "pid")

    val valid1 = A.filter { a -> a.keys.containsAll(need) }
    val ans1 = valid1.size

    println("Part 1: $ans1")
    printTime()

    markTime()

    val hclRegex = Regex("""^#[0-9a-f]{6}$""")
    val pidRegex = Regex("""^\d{9}$""")
    val eclSet = setOf("amb", "blu", "brn", "gry", "grn", "hzl", "oth")
    val ans2 = valid1.count { e ->
        e["byr"]!!.validateNumString(1920, 2002) &&
                e["iyr"]!!.validateNumString(2010, 2020) &&
                e["eyr"]!!.validateNumString(2020, 2030) &&
                e["hgt"]!!.validateHeight() &&
                e["hcl"]!!.matches(hclRegex) &&
                e["ecl"]!! in eclSet &&
                e["pid"]!!.matches(pidRegex)
    }

    println("Part 2: $ans2")
    printTime()
}

fun String.validateNumString(lo: Int, hi: Int) =
    runCatching {
        val int = toInt()
        int.toString() == this && int in lo..hi
    }.getOrElse { false }

fun String.validateHeight() = when {
    endsWith("cm") -> removeSuffix("cm").validateNumString(150, 193)
    endsWith("in") -> removeSuffix("in").validateNumString(59, 76)
    else -> false
}