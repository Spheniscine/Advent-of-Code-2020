@file:Suppress("NOTHING_TO_INLINE")

package commons

import kotlin.math.*

@Suppress("EqualsOrHashCode")
data class Vec3(val x: Int, val y: Int, val z: Int) {
    companion object {
        val ZERO = Vec3(0, 0, 0)
        inline val ORIGIN get() = ZERO
    }

    // Manhattan distance
    fun manDist(b: Vec3) = abs(x - b.x) + abs(y - b.y) + abs(z - b.z)
    fun manDist() = abs(x) + abs(y) + abs(z)

    operator fun plus(b: Vec3) = Vec3(x + b.x, y + b.y, z + b.z)
    operator fun minus(b: Vec3) = Vec3(x - b.x, y - b.y, z - b.z)
    operator fun times(scale: Int) = Vec3(x * scale, y * scale, z * scale)

    fun opposite() = Vec3(-x, -y, -z)
    inline operator fun unaryMinus() = opposite()

    operator fun get(i: Int) = when(i) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException()
    }

    override fun hashCode(): Int = hash(x.bitConcat(y), z.toLong()).toInt()
}

operator fun <V> Map<Vec3, V>.get(x: Int, y: Int, z: Int) = get(Vec3(x, y, z))
operator fun <V> MutableMap<Vec3, V>.set(x: Int, y: Int, z: Int, v: V) = set(Vec3(x, y, z), v)
operator fun <V: Any> NonNullMap<Vec3, V>.get(x: Int, y: Int, z: Int) = get(Vec3(x, y, z))