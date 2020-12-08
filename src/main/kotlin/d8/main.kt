package d8

import commons.*
import java.io.File

private val input by lazyInput(8, "gmail")

fun main() {
    println("--- Day 8: Handheld Halting ---")
    markTime()

    val lines = input.lines()

    val instructions = MutableList(lines.size) {
        val (code, arg) = lines[it].split(" ")
        Instruction(code, arg.removePrefix("+").toLong())
    }

    var vm = VM(instructions)
    vm.run()

    val ans1 = vm.acc

    println("Part 1: $ans1")
    printTime()

    markTime()

    for(i in instructions.indices) {
        val ins = instructions[i]
        val newIns = when(ins.code[0]) {
            'j' -> ins.copy(code = "nop")
            'n' -> ins.copy(code = "jmp")
            else -> continue
        }

        instructions[i] = newIns
        vm = VM(instructions)
        if(vm.run()) break
        instructions[i] = ins
    }

    val ans2 = vm.acc

    println("Part 2: $ans2")
    printTime()
}

data class Instruction(val code: String, val arg: Long)

class VM(val instructions: List<Instruction>) {
    var acc = 0L
    var ip = 0
    val vis = BooleanArray(instructions.size)

    // returns false if loop found
    fun run(): Boolean {
        while(true) {
            if(ip == instructions.size) return true
            if(vis[ip]) return false
            vis[ip] = true

            val (code, arg) = instructions[ip]
            when(code[0]) {
                'a' -> {
                    acc += arg
                }
                'j' -> {
                    ip += arg.toInt() - 1
                }
            }

            ip++
        }
    }
}