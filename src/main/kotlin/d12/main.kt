package d12

import commons.*
import java.io.File

private val input by lazyInput(12, "gmail")

fun main() {
    println("--- Day 12: Rain Risk ---")
    markTime()

    val lines = input.lines()

    var pos = Vec2.ORIGIN
    var dir = Dir2.East

    for(line in lines) {
        val c = line[0]
        val arg = line.drop(1).toInt()

        when(c) {
            in "NSEW" -> pos += Dir2.fromChar(c).vec * arg
            'L' -> dir -= arg/90
            'R' -> dir += arg/90
            'F' -> pos += dir.vec * arg
        }
    }

    val ans1 = pos.manDist()

    println("Part 1: $ans1")
    printTime()

    markTime()

    var waypoint = Vec2(10, -1)
    pos = Vec2.ORIGIN

    for(line in lines) {
        val c = line[0]
        val arg = line.drop(1).toInt()

        when(c) {
            in "NSEW" -> waypoint += Dir2.fromChar(c).vec * arg
            'L' -> waypoint = waypoint.rotate(-arg/90)
            'R' -> waypoint = waypoint.rotate(arg/90)
            'F' -> pos += waypoint * arg
        }
    }

    val ans2 = pos.manDist()

    println("Part 2: $ans2")
    printTime()
}