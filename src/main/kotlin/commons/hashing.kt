package commons

import kotlin.random.Random
import kotlin.math.*


/** splitmix64 pseudorandom permutation iterator, useful for custom hashing */
fun splitmix64(seed: Long): Long {
    var x = seed // * -7046029254386353131
    x = (x xor (x ushr 30)) * -4658895280553007687
    x = (x xor (x ushr 27)) * -7723592293110705685
    return (x xor (x ushr 31))
}
@JvmField val nonce64 = Random.nextLong()
@JvmField val gamma64 = Random.nextLong() or 1
fun Long.hash() = splitmix64(this * gamma64 + nonce64)

fun hash(a: Long, b: Long) = a.hash().xor(b).hash()

/** 32-bit variant, useful for mapping Int -> Int https://nullprogram.com/blog/2018/07/31/ */
fun splitmix32(seed: Int): Int {
    var x = seed // * 0x9e3779b9.toInt()
    x = (x xor (x ushr 16)) * 0x7feb352d
    x = (x xor (x ushr 15)) * 0x846ca68b.toInt()
    return (x xor (x ushr 16))
}
@JvmField val nonce32 = nonce64.toInt()
@JvmField val gamma32 = gamma64.toInt()
fun Int.hash() = splitmix32(this * gamma32 * nonce32)

private inline infix fun Int.rol(dist: Int) = shl(dist) or ushr(-dist)
val sipHasher by lazy { HalfSipHash() }
class HalfSipHash(val k0: Int = Random.nextInt(), val k1: Int = Random.nextInt()) {
    private var v0 = 0
    private var v1 = 0
    private var v2 = 0
    private var v3 = 0

    fun init() {
        v0 = k0; v1 = k1; v2 = 0x6c796765 xor k0; v3 = 0x74656462 xor k1
    }

    private fun round() {
        v0 += v1; v1 = v1 rol 5; v1 = v1 xor v0; v0 = v0 rol 16; v2 += v3; v3 = v3 rol 8; v3 = v3 xor v2
        v0 += v3; v3 = v3 rol 7; v3 = v3 xor v0; v2 += v1; v1 = v1 rol 13; v1 = v1 xor v2; v2 = v2 rol 16
    }

    fun acc(m: Int) {
        v3 = v3 xor m
        round()
        v0 = v0 xor m
    }

    fun acc(m: Long) {
        acc(m.toInt())
        acc(m.shr(32).toInt())
    }

    fun acc(input: String) {
        val len = input.length
        var m = 0
        for (i in 0 until len) {
            m = m or (input[i].toInt() shl 8*i)
            if(i and 3 == 3) {
                acc(m)
                m = 0
            }
        }
        m = m or (len shl 24)
        acc(m)
    }

    fun comma() {
        v1 = v1 xor 0xff
        round()
    }

    fun finish(): Int {
        v2 = v2 xor 0xee
        round(); round(); round()
        return v1 xor v3
    }

    fun finishLong(): Long {
        v2 = v2 xor 0xee
        round(); round(); round()
        val h = v1 xor v3
        v1 = v1 xor 0xdd
        round(); round(); round()
        return h.toLong().shl(32) or (v1 xor v3).toLong().and(0xffff_ffff)
    }

    inline fun doHash(block: HalfSipHash.() -> Unit): Long {
        init()
        block()
        return finishLong()
    }

    fun hash(input: String): Long {
        init()
        acc(input)
        return finishLong()
    }
}



