package d18

import commons.*
import java.io.File

private val input by lazyInput(18, "gmail")

fun main() {
    println("--- Day 18: Operation Order ---")
    markTime()

    val lines = input.lines()

    val ans1 = lines.sumOf { line ->
        val s = Solver()
        for(char in line) s.acc(char)
        s.ans()
    }

    println("Part 1: $ans1")
    printTime()

    markTime()

    val ans2 = lines.sumOf { line ->
        val s = Solver2()
        for(char in line) s.acc(char)
        s.ans()
    }

    println("Part 2: $ans2")
    printTime()
}

class Solver {
    val stk = mutableListOf<Token>()

    fun acc(num: Long) {
        if(stk.isEmpty()) stk.add(NumToken(num))
        else when(stk.last()) {
            TimesOp -> {
                stk.removeLast()
                val a = (stk.removeLast() as NumToken).num
                stk.add(NumToken(a * num))
            }
            PlusOp -> {
                stk.removeLast()
                val a = (stk.removeLast() as NumToken).num
                stk.add(NumToken(a + num))
            }
            is NumToken -> error("Can't accumulate two consecutive NumTokens")
            Paren -> stk.add(NumToken(num))
        }
    }

    fun acc(char: Char) {
        when(char) {
            in '0'..'9' -> acc((char - '0').toLong())
            '+' -> stk.add(PlusOp)
            '*' -> stk.add(TimesOp)
            '(' -> stk.add(Paren)
            ')' -> {
                val num = (stk.removeLast() as NumToken).num
                stk.removeLast().also { assert(it is Paren) }
                acc(num)
            }
        }
    }

    fun ans() = (stk.single() as NumToken).num
}

class Solver2 {
    val stk = mutableListOf<Token>()

    fun acc(num: Long) {
        if(stk.isEmpty()) stk.add(NumToken(num))
        else when(stk.last()) {
            PlusOp -> {
                stk.removeLast()
                val a = (stk.removeLast() as NumToken).num
                stk.add(NumToken(a + num))
            }
            is NumToken -> error("Can't accumulate two consecutive NumTokens")
            TimesOp, Paren -> stk.add(NumToken(num))
        }
    }

    fun acc(char: Char) {
        when(char) {
            in '0'..'9' -> acc((char - '0').toLong())
            '+' -> stk.add(PlusOp)
            '*' -> stk.add(TimesOp)
            '(' -> stk.add(Paren)
            ')' -> {
                var num = (stk.removeLast() as NumToken).num
                while(true) {
                    if(stk.removeLast() is Paren) break
                    num *= (stk.removeLast() as NumToken).num
                }
                acc(num)
            }
        }
    }

    init { acc('(') }

    fun ans(): Long {
        acc(')')
        return (stk.single() as NumToken).num
    }
}

interface Op
sealed class Token
object TimesOp: Op, Token()
object PlusOp: Op, Token()
data class NumToken(var num: Long): Token()
object Paren: Token()