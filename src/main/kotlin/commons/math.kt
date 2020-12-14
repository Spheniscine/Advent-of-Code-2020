@file:Suppress("NOTHING_TO_INLINE")

package commons

import kotlin.math.*

/** Saturation-cast to 32-bit */
fun Long.coerceToInt() = coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

fun Boolean.toLong() = if(this) 1L else 0L

/** Concatenates [this] 32-bit integer to an[other] 32-bit integer bitwise, and returns the result as a 64-bit integer. **/
fun Int.bitConcat(other: Int) = toLong().shl(32) or other.toLong().and(0xffff_ffff)

fun ByteArray.toLong() = longFromBytes(
    this[0], this[1], this[2], this[3], this[4], this[5], this[6], this[7])

fun longFromBytes(
    b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte, b7: Byte, b8: Byte
): Long {
    return (b1.toLong() and 0xFFL shl 56
            or (b2.toLong() and 0xFFL shl 48)
            or (b3.toLong() and 0xFFL shl 40)
            or (b4.toLong() and 0xFFL shl 32)
            or (b5.toLong() and 0xFFL shl 24)
            or (b6.toLong() and 0xFFL shl 16)
            or (b7.toLong() and 0xFFL shl 8)
            or (b8.toLong() and 0xFFL))
}

fun intFromBytes(
    b1: Byte, b2: Byte, b3: Byte, b4: Byte
): Int {
    return (b1.toInt() and 0xFF shl 24
            or (b2.toInt() and 0xFF shl 16)
            or (b3.toInt() and 0xFF shl 8)
            or (b4.toInt() and 0xFF))
}

inline fun <T> Iterable<T>.sumByLong(func: (T) -> Long) = fold(0L) { acc, t -> acc + func(t) }

tailrec fun gcd(a: Int, b: Int): Int = if(a == 0) abs(b) else gcd(b % a, a)
tailrec fun gcd(a: Long, b: Long): Long = if(a == 0L) abs(b) else gcd(b % a, a)

infix fun Int.modulo(mod: Int): Int = (this % mod).let { (it shr Int.SIZE_BITS - 1 and mod) + it }
infix fun Long.modulo(mod: Long) = (this % mod).let { (it shr Long.SIZE_BITS - 1 and mod) + it }
infix fun Long.modulo(mod: Int) = modulo(mod.toLong()).toInt()

infix fun Int.divCeil(other: Int) =
    (this / other).let { if(xor(other) >= 0 && it * other != this) it+1 else it }

inline infix fun Int.divFloor(other: Int) = Math.floorDiv(this, other)

infix fun Long.divCeil(other: Long) =
    (this / other).let { if(xor(other) >= 0 && it * other != this) it+1 else it }

inline infix fun Long.divFloor(other: Long) = Math.floorDiv(this, other)

// Solves system: x = a1 (mod m1), x = a2 (mod m2)
// yields in form (x mod lcm, lcm). If impossible, yields (-1, 0)
// prereq: lcm(m1, m2) is in Long; m1, m2 > 0
inline fun <R> crt(a1: Long, m1: Long, a2: Long, m2: Long, yield: (x: Long, lcm: Long) -> R): R {
    require(m1 > 0 && m2 > 0) { "moduli must be > 0" }
    @Suppress("NAME_SHADOWING") val a1 = a1 modulo m1
    @Suppress("NAME_SHADOWING") val a2 = a2 modulo m2

    val g = gcd(m1, m2)
    val r = a1 % g
    if(r != a2 % g) return yield(-1, 0)
    val p1 = m1 / g
    val p2 = m2 / g
    val b1 = a1 / g
    val b2 = a2 / g

    val pi = p1.modInv(p2)
    val x = ((b2 - b1) * pi modulo p2) * p1 + b1
    return yield(x*g+r, p1*p2*g)
}

fun crt(a1: Long, m1: Long, a2: Long, m2: Long) = crt(a1, m1, a2, m2) { x, _ -> x }

inline fun Long.modInvOrElse(base: Long, default: () -> Long): Long {
    var s = 0L
    var os = 1L
    var r = base
    var or = modulo(base)

    while(r != 0L) {
        val q = or / r
        or = r.also { r = or - q * r }
        os = s.also { s = os - q * s }
    }

    return if(or != 1L) default()
    else os.let { if(it >= 0) it else it + base }
}

fun Long.modInv(base: Long) = modInvOrElse(base) { throw ArithmeticException("$this has no inverse in modulo $base") }


