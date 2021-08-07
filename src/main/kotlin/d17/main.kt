package d17

import commons.*

private val input by lazyInput(17, "gmail")

fun main() {
    println("--- Day 17: Conway Cubes ---")
    markTime()

    val lines = input.lines()
    val init = HashSet<Vec3>()

    for(y in lines.indices) for(x in lines[y].indices) {
        if(lines[y][x] == '#') init.add(Vec3(x, y, 0))
    }

    var curr: Set<Vec3> = init

    repeat(6) { curr = curr.next() }

    val ans1 = curr.size

    println("Part 1: $ans1")
    printTime()

    markTime()

    val init2 = init.mapTo(HashSet()) { (x, y, z) ->
        Vec4(x, y, z, 0)
    }

    var curr2: Set<Vec4> = init2
    repeat(6) { curr2 = curr2.next() }
    val ans2 = curr2.size

    println("Part 2: $ans2")
    printTime()
}

fun Set<Vec3>.next(): Set<Vec3> {
    val cnt = HashMap<Vec3, Int>().default(0)
    for((x, y, z) in this) {
        for(nx in x-1..x+1) for(ny in y-1..y+1) for(nz in z-1..z+1) {
            cnt[nx, ny, nz]++
        }
    }

    val ite = cnt.iterator()
    for((vec, c) in ite) {
        if(!(c == 3 || c == 4 && vec in this)) ite.remove()
    }

    return cnt.keys
}

@JvmName("next4")
fun Set<Vec4>.next(): Set<Vec4> {
    val cnt = HashMap<Vec4, Int>().default(0)
    for((x, y, z, w) in this) {
        for(nx in x-1..x+1) for(ny in y-1..y+1) for(nz in z-1..z+1) for(nw in w-1..w+1) {
            cnt[Vec4(nx, ny, nz, nw)]++
        }
    }

    val ite = cnt.iterator()
    for((vec, c) in ite) {
        if(!(c == 3 || c == 4 && vec in this)) ite.remove()
    }

    return cnt.keys
}

@Suppress("EqualsOrHashCode")
data class Vec4(val x: Int, val y: Int, val z: Int, val w: Int) {
    override fun hashCode(): Int = sipHasher.run {
        init()
        acc(x); acc(y); acc(z); acc(w)
        finish()
    }
}