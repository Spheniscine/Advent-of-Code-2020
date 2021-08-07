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

interface HashingStrategy<K> {
    fun hash(key: K): Long
    fun equals(a: K, b: K): Boolean
}
inline fun <K> HashingStrategy(crossinline hash: (K) -> Long,
                               crossinline equals: (K, K) -> Boolean) =
    object: HashingStrategy<K> {
        override fun hash(key: K) = hash(key)
        override fun equals(a: K, b: K) = equals(a, b)
    }
inline fun <K> HashingStrategy(crossinline hash: (K) -> Long) = HashingStrategy(hash) { a, b -> a == b }

@Suppress("UNCHECKED_CAST")
class CustomHashMap<K, V>(val strategy: HashingStrategy<K>, capacity: Int = 8, val linked: Boolean = false): AbstractMutableMap<K, V>() {
    companion object {
        private const val REBUILD_LENGTH_THRESHOLD = 32
        private const val FREE: Byte = 0
        private const val REMOVED: Byte = 1
        private const val FILLED: Byte = 2
    }

    override var size: Int = 0
        private set

    private lateinit var keyArr: Array<K?>
    private lateinit var hashArr: LongArray
    private lateinit var valArr: Array<V?>
    private lateinit var statusArr: ByteArray
    private var removedCount = 0
    private var mask = 0
    private inline val head get() = mask + 1
    private var modCount = 0

    // only initialized if linked = true
    private lateinit var next: IntArray
    private lateinit var prev: IntArray

    init {
        require(capacity >= 0) { "Capacity must be non-negative" }
        val length = Integer.highestOneBit(4 * max(1, capacity) - 1)
        // Length is a power of 2 now
        initEmptyTable(length)
    }

    private fun initEmptyTable(length: Int) {
        keyArr = arrayOfNulls<Any>(length) as Array<K?>
        hashArr = LongArray(length)
        valArr = arrayOfNulls<Any>(length) as Array<V?>
        statusArr = ByteArray(length)
        size = 0
        removedCount = 0
        mask = length - 1

        if(linked) {
            next = IntArray(length+1).also { it[head] = head }
            prev = IntArray(length+1).also { it[head] = head }
        }
    }

    override fun containsKey(key: K): Boolean = containsKey(key, strategy.hash(key))
    private fun containsKey(key: K, hash: Long): Boolean {
        var pos = hash.toInt() and mask
        while(statusArr[pos] != FREE) {
            if(statusArr[pos] == FILLED && hashArr[pos] == hash && strategy.equals(keyArr[pos] as K, key)) {
                return true
            }
            pos = pos + 1 and mask
        }
        return false
    }


    private inline fun iter(act: (k: K, v: V) -> Unit) {
        if(linked) {
            var i = next[head]
            while(i != head) {
                act(keyArr[i] as K, valArr[i] as V)
                i = next[i]
            }
        } else {
            for(i in keyArr.indices) {
                if(statusArr[i] == FILLED) act(keyArr[i] as K, valArr[i] as V)
            }
        }
    }

    private abstract inner class MapIterator<X> : MutableIterator<X> {
        private var i = when {
            isEmpty() -> head
            linked -> next[head]
            else -> statusArr.indexOf(FILLED)
        }
        private var j = -1
        private val curMods = modCount

        override fun hasNext(): Boolean {
            if(curMods != modCount) throw ConcurrentModificationException()
            return i != head
        }

        protected abstract fun returnItem(pos: Int): X
        override fun next(): X {
            if(curMods != modCount) throw ConcurrentModificationException()
            if(i == head) throw NoSuchElementException()
            j = i
            if(linked) i = next[i]
            else do { i++ } while(i != head && statusArr[i] != FILLED)
            return returnItem(j)
        }

        override fun remove() {
            if(curMods != modCount) throw ConcurrentModificationException()
            if(j == -1) throw IllegalStateException("Item to remove doesn't exist")
            if(statusArr[j] == REMOVED) throw IllegalStateException("Item already removed")
            statusArr[j] = REMOVED
            size--
            removedCount++
            if(linked) {
                next[prev[j]] = next[j]
                prev[next[j]] = prev[j]
            }
        }
    }

    operator fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = object : MapIterator<MutableMap.MutableEntry<K, V>>() {
        override fun returnItem(pos: Int) = object: MutableMap.MutableEntry<K, V> {
            private inline val map get() = this@CustomHashMap
            override val key: K = keyArr[pos] as K
            override var value: V = valArr[pos] as V
                private set

            override fun setValue(newValue: V): V {
                val oldValue = value
                map[key] = newValue
                value = newValue
                return oldValue
            }

            override fun equals(other: Any?): Boolean = other === this ||
                    other is Map.Entry<*, *> && key == other.key && value == other.value

            override fun hashCode(): Int = key.hashCode() xor value.hashCode()
            override fun toString(): String = "$key=$value"
        }
    }

    fun keyIterator(): MutableIterator<K> = object : MapIterator<K>() {
        override fun returnItem(pos: Int): K = keyArr[pos] as K
    }
    fun valueIterator(): MutableIterator<V> = object : MapIterator<V>() {
        override fun returnItem(pos: Int): V = valArr[pos] as V
    }

    override fun containsValue(value: V): Boolean {
        iter { _, v ->
            if(v == value) return true
        }
        return false
    }

    override fun get(key: K): V? = get(key, strategy.hash(key))
    private fun get(key: K, hash: Long): V? {
        var pos = hash.toInt() and mask
        while(statusArr[pos] != FREE) {
            if(statusArr[pos] == FILLED && hashArr[pos] == hash && strategy.equals(keyArr[pos] as K, key)) {
                return valArr[pos]
            }
            pos = pos + 1 and mask
        }
        return null
    }

    private inner class EntrySet: AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        private inline val map get() = this@CustomHashMap
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = map.put(element.key, element.value) != element.value
        override fun clear() = map.clear()
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = map.iterator()
        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            if(contains(element)) {
                map.remove(element.key)
                return true
            }
            return false
        }
        override val size: Int get() = map.size
        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean =
            map.containsKey(element.key) && map[element.key] == element.value
    }
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> by lazy { EntrySet() }
    private inner class KeySet : AbstractMutableSet<K>() {
        private inline val map get() = this@CustomHashMap
        override fun add(element: K): Boolean { throw UnsupportedOperationException() }
        override fun clear() = map.clear()
        override fun iterator() = map.keyIterator()
        override fun remove(element: K): Boolean {
            if(contains(element)) {
                map.remove(element)
                return true
            }
            return false
        }
        override val size: Int get() = map.size
        override fun contains(element: K): Boolean = map.containsKey(element)
    }
    override val keys: MutableSet<K> by lazy { KeySet() }
    private inner class ValueCollection: AbstractMutableCollection<V>() {
        private inline val map get() = this@CustomHashMap
        override val size: Int get() = map.size
        override fun contains(element: V): Boolean = map.containsValue(element)
        override fun add(element: V): Boolean { throw UnsupportedOperationException() }
        override fun clear() = map.clear()
        override fun iterator() = map.valueIterator()
        override fun remove(element: V): Boolean {
            iter { k, v ->
                if(v == element) {
                    map.remove(k)
                    return true
                }
            }
            return false
        }
    }
    override val values: MutableCollection<V> by lazy { ValueCollection() }

    override fun clear() {
        modCount++
        if(keyArr.size > REBUILD_LENGTH_THRESHOLD) {
            initEmptyTable(REBUILD_LENGTH_THRESHOLD)
        } else {
            statusArr.fill(FREE)
            size = 0
            removedCount = 0
            if(linked) {
                next[head] = head
                prev[head] = head
            }
        }
    }

    private fun appendEntry(i: Int) {
        if(linked) {
            val last = prev[head]
            next[last] = i
            prev[i] = last
            next[i] = head
            prev[head] = i
        }
    }

    private fun rebuild(newLength: Int) {
        val oldKeys = keyArr
        val oldValues = valArr

        if(linked) {
            val oldNext = next
            val oldHead = head
            initEmptyTable(newLength)
            var i = oldNext[oldHead]
            while(i != oldHead) {
                put(oldKeys[i] as K, oldValues[i] as V)
                i = oldNext[i]
            }
        } else {
            val oldStatus = statusArr
            initEmptyTable(newLength)
            for (i in oldKeys.indices) {
                if (oldStatus[i] == FILLED) {
                    put(oldKeys[i] as K, oldValues[i] as V)
                }
            }
        }
    }

    override fun put(key: K, value: V): V? = put(key, value, strategy.hash(key))
    private fun put(key: K, value: V, hash: Long): V? {
        var pos = hash.toInt() and mask
        while(statusArr[pos] == FILLED) {
            if(hashArr[pos] == hash && strategy.equals(keyArr[pos] as K, key)) {
                val oldValue = valArr[pos]
                valArr[pos] = value
                return oldValue
            }
            pos = pos + 1 and mask
        }
        if(statusArr[pos] == FREE) {
            modCount++
            statusArr[pos] = FILLED
            keyArr[pos] = key
            hashArr[pos] = hash
            valArr[pos] = value
            size++
            appendEntry(pos)
            if((size + removedCount) * 2 > keyArr.size) {
                rebuild(keyArr.size * 2) // enlarge the table
            }
            return null
        }
        val removedPos = pos
        pos = pos + 1 and mask
        while(statusArr[pos] != FREE) {
            if(statusArr[pos] == FILLED && hashArr[pos] == hash && strategy.equals(keyArr[pos] as K, key)) {
                val oldValue = valArr[pos]
                valArr[pos] = value
                return oldValue
            }
            pos = pos + 1 and mask
        }
        modCount++
        statusArr[removedPos] = FILLED
        keyArr[removedPos] = key
        hashArr[removedPos] = hash
        valArr[removedPos] = value
        size++
        removedCount--
        appendEntry(removedPos)
        return null
    }

    override fun remove(key: K): V? = remove(key, strategy.hash(key))
    private fun remove(key: K, hash: Long): V? {
        var pos = hash.toInt() and mask
        while(statusArr[pos] != FREE) {
            if(statusArr[pos] == FILLED && hashArr[pos] == hash && strategy.equals(keyArr[pos] as K, key)) {
                modCount++
                val removedValue = valArr[pos]
                statusArr[pos] = REMOVED
                size--
                removedCount++
                if(linked) {
                    next[prev[pos]] = next[pos]
                    prev[next[pos]] = prev[pos]
                }
                if(keyArr.size > REBUILD_LENGTH_THRESHOLD) {
                    if(8 * size <= keyArr.size) {
                        rebuild(keyArr.size / 2)
                    } else if(size < removedCount) {
                        rebuild(keyArr.size)
                    }
                }
                return removedValue
            }
            pos = pos + 1 and mask
        }
        return null
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('{')
        iter { k, v ->
            if(sb.length > 1) sb.append(", ")
            sb.append(k)
            sb.append('=')
            sb.append(v)
        }
        sb.append('}')
        return sb.toString()
    }
}

class CustomHashSet<E>(val strategy: HashingStrategy<E>, capacity: Int = 8, val linked: Boolean = false):
    MapBackedSet<E>(CustomHashMap(strategy, capacity, linked))

//val STRING_HASHING_STRATEGY = HashingStrategy<String> { k -> k.hash() }

val STRING_HASHING_STRATEGY = HashingStrategy<String> { k ->
    sipHasher.run {
        init()
        acc(k)
        finishLong()
    }
}

fun <V> StringHashMap(linked: Boolean = false) = CustomHashMap<String, V>(STRING_HASHING_STRATEGY, linked = linked)
fun StringHashSet(linked: Boolean = false) = CustomHashSet(STRING_HASHING_STRATEGY, linked = linked)

abstract class MapBackedSet<T>(val _map: MutableMap<T, Unit>): AbstractMutableSet<T>() {
    override val size: Int get() = _map.size
    override fun add(element: T): Boolean = _map.put(element, Unit) == null
    override fun remove(element: T): Boolean = _map.remove(element) == Unit
    override fun clear() { _map.clear() }
    override fun contains(element: T) = _map.containsKey(element)
    override fun iterator() = _map.keys.iterator()
}

