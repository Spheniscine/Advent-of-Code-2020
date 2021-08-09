package commons

import kotlin.math.*

interface HashingStrategy<K> {
    fun hash(key: K): Long
    fun equals(a: K, b: K): Boolean
}
inline fun <K> HashingStrategy(crossinline hash: (K) -> Long,
                               crossinline equals: (K, K) -> Boolean) =
    object: HashingStrategy<K> {
        // keep last hash to minimize need for rehashing
        private var hasLast = false
        private var lastKey: K? = null
        private var lastHash = 0L

        override fun hash(key: K): Long {
            if(hasLast && key === lastKey) return lastHash
            hasLast = true
            lastKey = key
            lastHash = hash(key)
            return lastHash
        }
        override fun equals(a: K, b: K) = equals(a, b)
    }
inline fun <K> HashingStrategy(crossinline hash: (K) -> Long) = HashingStrategy(hash) { a, b -> a == b }


class CustomHashMap<K, V>(val strategy: HashingStrategy<K>, capacity: Int = 8, val linked: Boolean = false): AbstractMutableMap<K, V>() {
    companion object {
        private const val REBUILD_LENGTH_THRESHOLD = 32
        private const val FREE: Byte = 0
        private const val REMOVED: Byte = 1
        private const val FILLED: Byte = 2
    }

    override var size: Int = 0
        private set

    private inner class Entry(
        override val key: K,
        val hash: Long,
        override var value: V
    ) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val old = value
            value = newValue
            return old
        }

        fun matches(key: K, hash: Long) = hash == this.hash && strategy.equals(key, this.key)

        override fun equals(other: Any?): Boolean = other === this ||
                other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()
        override fun toString(): String = "$key=$value"
    }

    private lateinit var arr: Array<Entry?>
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
        arr = arrayOfNulls(length)
        statusArr = ByteArray(length)
        size = 0
        removedCount = 0
        mask = length - 1

        if (linked) {
            next = IntArray(length + 1).also { it[head] = head }
            prev = IntArray(length + 1).also { it[head] = head }
        }
    }

    override fun containsKey(key: K): Boolean = containsKey(key, strategy.hash(key))
    private fun containsKey(key: K, hash: Long): Boolean {
        var pos = hash.toInt() and mask
        var step = 0
        while (statusArr[pos] != FREE) {
            if (statusArr[pos] == FILLED && arr[pos]!!.matches(key, hash)) {
                return true
            }
            pos = pos + ++step and mask
        }
        return false
    }


    private inline fun iter(act: (Entry) -> Unit) {
        if (linked) {
            var i = next[head]
            while (i != head) {
                act(arr[i]!!)
                i = next[i]
            }
        } else {
            for (i in arr.indices) {
                if (statusArr[i] == FILLED) act(arr[i]!!)
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
        private var expectedModCount = modCount

        override fun hasNext(): Boolean {
            if (expectedModCount != modCount) throw ConcurrentModificationException()
            return i != head
        }

        protected abstract fun returnItem(pos: Int): X
        override fun next(): X {
            if (expectedModCount != modCount) throw ConcurrentModificationException()
            if (i == head) throw NoSuchElementException()
            j = i
            if (linked) i = next[i]
            else do {
                i++
            } while (i != head && statusArr[i] != FILLED)
            return returnItem(j)
        }

        override fun remove() {
            if (expectedModCount != modCount) throw ConcurrentModificationException()
            if (j == -1) throw IllegalStateException("Item to remove doesn't exist")
            if (statusArr[j] == REMOVED) throw IllegalStateException("Item already removed")
            expectedModCount++
            modCount++
            removeEntry(j)
            // note: doesn't rebuild table
        }
    }

    operator fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> =
        object : MapIterator<MutableMap.MutableEntry<K, V>>() {
            override fun returnItem(pos: Int) = arr[pos]!!
        }

    fun keyIterator(): MutableIterator<K> = object : MapIterator<K>() {
        override fun returnItem(pos: Int): K = arr[pos]!!.key
    }

    fun valueIterator(): MutableIterator<V> = object : MapIterator<V>() {
        override fun returnItem(pos: Int): V = arr[pos]!!.value
    }

    override fun containsValue(value: V): Boolean {
        iter {
            if (it.value == value) return true
        }
        return false
    }

    override fun get(key: K): V? = get(key, strategy.hash(key))
    private fun get(key: K, hash: Long): V? {
        var pos = hash.toInt() and mask
        var step = 0
        while (statusArr[pos] != FREE) {
            arr[pos]?.let { entry ->
                if (entry.matches(key, hash)) return entry.value
            }
            pos = pos + ++step and mask
        }
        return null
    }

    private inner class EntrySet : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        private inline val map get() = this@CustomHashMap
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
            map.put(element.key, element.value) != element.value

        override fun clear() = map.clear()
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = map.iterator()
        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            if (contains(element)) {
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
        override fun add(element: K): Boolean {
            throw UnsupportedOperationException()
        }

        override fun clear() = map.clear()
        override fun iterator() = map.keyIterator()
        override fun remove(element: K): Boolean {
            if (contains(element)) {
                map.remove(element)
                return true
            }
            return false
        }

        override val size: Int get() = map.size
        override fun contains(element: K): Boolean = map.containsKey(element)
    }

    override val keys: MutableSet<K> by lazy { KeySet() }

    private inner class ValueCollection : AbstractMutableCollection<V>() {
        private inline val map get() = this@CustomHashMap
        override val size: Int get() = map.size
        override fun contains(element: V): Boolean = map.containsValue(element)
        override fun add(element: V): Boolean {
            throw UnsupportedOperationException()
        }

        override fun clear() = map.clear()
        override fun iterator() = map.valueIterator()
        override fun remove(element: V): Boolean {
            iter {
                if (it.value == element) {
                    map.remove(it.key)
                    return true
                }
            }
            return false
        }
    }

    override val values: MutableCollection<V> by lazy { ValueCollection() }

    override fun clear() {
        modCount++
        if (arr.size > REBUILD_LENGTH_THRESHOLD) {
            initEmptyTable(REBUILD_LENGTH_THRESHOLD)
        } else {
            arr.fill(null)
            statusArr.fill(FREE)
            size = 0
            removedCount = 0
            if (linked) {
                next[head] = head
                prev[head] = head
            }
        }
    }

    private fun rebuild(newLength: Int) {
        val oldArr = arr

        if (linked) {
            val oldNext = next
            val oldHead = head
            initEmptyTable(newLength)
            var i = oldNext[oldHead]
            while (i != oldHead) {
                reinsert(oldArr[i]!!)
                i = oldNext[i]
            }
        } else {
            initEmptyTable(newLength)
            for (entry in oldArr) {
                if (entry != null) reinsert(entry)
            }
        }
    }

    /** only use in rebuild */
    private fun reinsert(entry: Entry) {
        var pos = entry.hash.toInt() and mask
        var step = 0
        while (statusArr[pos] == FILLED) pos = pos + ++step and mask
        addEntry(entry, pos)
    }

    private fun addEntry(entry: Entry, pos: Int) {
        statusArr[pos] = FILLED
        arr[pos] = entry
        size++
        if (linked) {
            val last = prev[head]
            next[last] = pos
            prev[pos] = last
            next[pos] = head
            prev[head] = pos
        }
    }

    override fun put(key: K, value: V): V? = put(key, value, strategy.hash(key))
    private fun put(key: K, value: V, hash: Long): V? {
        var pos = hash.toInt() and mask
        var step = 0
        while (statusArr[pos] == FILLED) {
            arr[pos]?.let { entry ->
                if (entry.matches(key, hash)) {
                    return entry.setValue(value)
                }
            }
            pos = pos + ++step and mask
        }
        if (statusArr[pos] == FREE) {
            modCount++
            addEntry(Entry(key, hash, value), pos)
            if ((size + removedCount) * 2 > arr.size) {
                if(size > removedCount) rebuild(arr.size * 2) // enlarge the table
                else rebuild(arr.size)
            }
            return null
        }
        val removedPos = pos
        pos = pos + ++step and mask
        while (statusArr[pos] != FREE) {
            arr[pos]?.let { entry ->
                if (entry.matches(key, hash)) {
                    return entry.setValue(value)
                }
            }
            pos = pos + ++step and mask
        }
        modCount++
        addEntry(Entry(key, hash, value), removedPos)
        removedCount--
        return null
    }

    override fun remove(key: K): V? = remove(key, strategy.hash(key))
    private fun remove(key: K, hash: Long): V? {
        var pos = hash.toInt() and mask
        var step = 0
        while (statusArr[pos] != FREE) {
            arr[pos]?.let { entry ->
                if (entry.matches(key, hash)) {
                    modCount++
                    removeEntry(pos)
                    if (arr.size > REBUILD_LENGTH_THRESHOLD) {
                        if (8 * size <= arr.size) {
                            rebuild(arr.size / 2)
                        } //else if (size < removedCount) {
//                            rebuild(arr.size)
//                        }
                    }
                    return entry.value
                }
            }
            pos = pos + ++step and mask
        }
        return null
    }

    private fun removeEntry(pos: Int) {
        statusArr[pos] = REMOVED
        arr[pos] = null
        size--
        removedCount++
        if (linked) {
            next[prev[pos]] = next[pos]
            prev[next[pos]] = prev[pos]
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('{')
        iter { (k, v) ->
            if (sb.length > 1) sb.append(", ")
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