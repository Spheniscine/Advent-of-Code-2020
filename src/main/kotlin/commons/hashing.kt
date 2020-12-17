package commons

import kotlin.random.Random

val _seed1 = Random.nextLong()

/** splitmix64 pseudorandom permutation iterator, useful for custom hashing */
fun splitmix64(seed: Long): Long {
    var x = seed * -7046029254386353131
    x = (x xor (x ushr 30)) * -4658895280553007687
    x = (x xor (x ushr 27)) * -7723592293110705685
    return (x xor (x ushr 31))
}
fun Long.hash() = splitmix64(_seed1 xor this)

fun hash(a: Long, b: Long) = splitmix64(a.hash().xor(b))

fun splitmix32(seed: Int): Int {
    var x = seed * 0x9e3779b9.toInt()
    x = (x xor (x ushr 16)) * 0x7feb352d
    x = (x xor (x ushr 15)) * 0x846ca68b.toInt()
    return (x xor (x ushr 16))
}
@JvmField val nonce32 = _seed1.toInt()
fun Int.hash() = splitmix32(nonce32 xor this)

private inline infix fun Int.rol(dist: Int) = shl(dist) or ushr(-dist)
val sipHasher by lazy { HalfSipHash() }
class HalfSipHash(val k0: Int = _seed1.toInt(), val k1: Int = _seed1.shr(32).toInt()) {
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

    private inline fun ByteArray.getOrFF(i: Int) = if(i < size) get(i).toInt() and 0xff else 0xff

    fun acc(input: String) {
        val bytes = input.toByteArray()
        val len = bytes.size
        for (i in 0 until len step 4) {
            val int = (bytes[i].toInt() shl 24
                    or (bytes.getOrFF(i+1) shl 16)
                    or (bytes.getOrFF(i+2) shl 8)
                    or bytes.getOrFF(i+3))
            acc(int)
        }
        comma()
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

    inline fun doHash(block: HalfSipHash.() -> Unit): Int {
        init()
        block()
        return finish()
    }

    fun hash(input: IntArray): Int {
        init()
        for (m in input) acc(m)
        comma()
        return finish()
    }

    fun hash(input: LongArray): Int {
        init()
        for (m in input) acc(m)
        comma()
        return finish()
    }

    fun hash(input: String): Int {
        init()
        acc(input)
        return finish()
    }
}

abstract class WrappedKeyMap<K, W, V>(val _del: MutableMap<W, V> = HashMap()): AbstractMutableMap<K, V>() {
    abstract fun wrap(key: K): W
    abstract fun unwrap(key: W): K

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
            by lazy {
                object: AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
                    override val size
                        get() = _del.size

                    override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = _del.put(wrap(element.key), element.value) != element.value

                    override fun iterator() = object: MutableIterator<MutableMap.MutableEntry<K, V>> {
                        private val delIterator = _del.iterator()

                        override fun hasNext(): Boolean = delIterator.hasNext()

                        override fun next() = object: MutableMap.MutableEntry<K, V> {
                            private val delEntry = delIterator.next()
                            override val key: K
                                get() = unwrap(delEntry.key)
                            override val value: V
                                get() = delEntry.value

                            override fun setValue(newValue: V): V = delEntry.setValue(newValue)
                        }

                        override fun remove() {
                            delIterator.remove()
                        }
                    }

                    override fun clear() { _del.clear() }
                    override fun contains(element: MutableMap.MutableEntry<K, V>) =
                        wrap(element.key).let { _del.containsKey(it) && _del[it] == element.value }
                }
            }


    override val size get() = _del.size
    override val values get() = _del.values
    override fun clear() = _del.clear()
    override fun containsKey(key: K) = _del.containsKey(wrap(key))
    override fun containsValue(value: V) = _del.containsValue(value)
    override fun get(key: K) = _del[wrap(key)]
    override fun put(key: K, value: V): V? = _del.put(wrap(key), value)
    override fun remove(key: K) = _del.remove(wrap(key))
}

open class StringHashMap<V>(_del: HashMap<Hash, V> = HashMap()) : WrappedKeyMap<String, StringHashMap.Hash, V>(_del) {
    override fun wrap(key: String): Hash = Hash(key)
    override fun unwrap(key: Hash): String = key.data

    class Hash(val data: String) {
        override fun hashCode(): Int = sipHasher.hash(data)
        override fun equals(other: Any?) =
            other is Hash && data == other.data
    }
}

open class LongHashMap<V>(_del: HashMap<Hash, V> = HashMap()) : WrappedKeyMap<Long, LongHashMap.Hash, V>(_del) {
    override fun wrap(key: Long): Hash = Hash(key)
    override fun unwrap(key: Hash): Long = key.data

    class Hash(val data: Long) {
        override fun hashCode(): Int = data.hash().toInt()
        override fun equals(other: Any?) =
            other is Hash && data == other.data
    }
}

open class IntHashMap<V>(_del: HashMap<Hash, V> = HashMap()) : WrappedKeyMap<Int, IntHashMap.Hash, V>(_del) {
    override fun wrap(key: Int): Hash = Hash(key)
    override fun unwrap(key: Hash): Int = key.data

    class Hash(val data: Int) {
        override fun hashCode(): Int = data.hash()
        override fun equals(other: Any?) =
            other is Hash && data == other.data
    }
}

abstract class MapBackedSet<T>(val _map: MutableMap<T, Unit>): AbstractMutableSet<T>() {
    override val size: Int get() = _map.size
    override fun add(element: T): Boolean = _map.put(element, Unit) == null
    override fun remove(element: T): Boolean = _map.remove(element) == Unit
    override fun clear() { _map.clear() }
    override fun contains(element: T) = _map.containsKey(element)
    override fun iterator() = object: MutableIterator<T> {
        val mapIterator = _map.iterator()
        override fun hasNext(): Boolean = mapIterator.hasNext()
        override fun next(): T = mapIterator.next().key
        override fun remove() = mapIterator.remove()
    }
}

open class StringHashSet(_map: MutableMap<String, Unit> = StringHashMap()): MapBackedSet<String>(_map)

open class LongHashSet(_map: MutableMap<Long, Unit> = LongHashMap()): MapBackedSet<Long>(_map)

open class IntHashSet(_map: MutableMap<Int, Unit> = IntHashMap()): MapBackedSet<Int>(_map)