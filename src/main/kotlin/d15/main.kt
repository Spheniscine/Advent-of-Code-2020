package d15

import commons.*
import kotlin.math.*
import kotlin.random.Random

private val input by lazyInput(15, "gmail")

fun main() {
    println("--- Day 15 ---")
    markTime()

    val A = input.split(',').map { it.toInt() }
    val map = IntIntMap(nullValue = -1)

    for(i in 0 until A.lastIndex) {
        map[A[i]] = i
    }

    var a = A.last()

    for(i in A.size until 2020) {
        val p = map[a]
        map[a] = i-1
        a = if(p == -1) 0 else i-1-p
    }

    val ans1 = a

    println("Part 1: $ans1")
    printTime()

    markTime()

    for(i in 2020 until 30000000) {
        val p = map[a]
        map[a] = i-1
        a = if(p == -1) 0 else i-1-p
    }

    val ans2 = a

    println("Part 2: $ans2")
    printTime()
}

typealias IntIntMap = _Ez_Int__Int_HashMap
inline operator fun IntIntMap.set(key: Int, value: Int) { put(key, value) }
inline operator fun IntIntMap.contains(key: Int) = containsKey(key)

class _Ez_Int__Int_HashMap(capacity: Int = DEFAULT_CAPACITY, val nullValue: Int = DEFAULT_NULL_VALUE) :
    _Ez_Int__Int_Map {
    companion object {
        private const val DEFAULT_CAPACITY = 8
        // There are three invariants for size, removedCount and arraysLength:
// 1. size + removedCount <= 1/2 arraysLength
// 2. size > 1/8 arraysLength
// 3. size >= removedCount
// arraysLength can be only multiplied by 2 and divided by 2.
// Also, if it becomes >= 32, it can't become less anymore.
        private const val REBUILD_LENGTH_THRESHOLD = 32
        private const val HASHCODE_INITIAL_VALUE = -0x7ee3623b
        private const val HASHCODE_MULTIPLIER = 0x01000193
        private const val FREE: Byte = 0
        private const val REMOVED: Byte = 1
        private const val FILLED: Byte = 2
        private const  val   DEFAULT_NULL_VALUE = 0
        private val hashSeed = Random.nextInt()
    }

    private lateinit   var keys: IntArray
    private lateinit   var values: IntArray
    private lateinit var status: ByteArray
    override var size = 0
        private set
    private var removedCount = 0
    private var mask = 0

    constructor(map: _Ez_Int__Int_Map) : this(map.size) {
        val it = map.iterator()
        while (it.hasNext()) {
            put(it.key, it.value)
            it.next()
        }
    }

    constructor(javaMap: Map<Int, Int>) : this(javaMap.size) {
        for ((key, value) in javaMap) {
            put(key, value)
        }
    }

    private fun getStartPos(h: Int): Int {
        var x = (h xor hashSeed) * 0x9e3779b9.toInt()
        x = (x xor (x ushr 16)) * 0x7feb352d
        x = (x xor (x ushr 15)) * 0x846ca68b.toInt()
        return (x xor (x ushr 16)) and mask
    }

    override fun isEmpty(): Boolean = size == 0

    override fun containsKey(
        key: Int
    ): Boolean {
        var pos = getStartPos(key)
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                return true
            }
            pos = pos + 1 and mask
        }
        return false
    }

    override fun  get(
        key: Int
    ): Int {
        var pos = getStartPos(key)
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                return values[pos]
            }
            pos = pos + 1 and mask
        }
        return nullValue
    }

    override fun  put(
        key: Int,
        value: Int
    ): Int {
        var pos = getStartPos(key)
        while (status[pos] == FILLED) {
            if (keys[pos] == key) {
                val   oldValue = values[pos]
                values[pos] = value
                return oldValue
            }
            pos = pos + 1 and mask
        }
        if (status[pos] == FREE) {
            status[pos] = FILLED
            keys[pos] = key
            values[pos] = value
            size++
            if ((size + removedCount) * 2 > keys.size) {
                rebuild(keys.size * 2) // enlarge the table
            }
            return nullValue
        }
        val removedPos = pos
        pos = pos + 1 and mask
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                val   oldValue = values[pos]
                values[pos] = value
                return oldValue
            }
            pos = pos + 1 and mask
        }
        status[removedPos] = FILLED
        keys[removedPos] = key
        values[removedPos] = value
        size++
        removedCount--
        return nullValue
    }

    override fun  remove(
        key: Int
    ): Int {
        var pos = getStartPos(key)
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                val   removedValue = values[pos]
                status[pos] = REMOVED
                size--
                removedCount++
                if (keys.size > REBUILD_LENGTH_THRESHOLD) {
                    if (8 * size <= keys.size) {
                        rebuild(keys.size / 2) // compress the table
                    } else if (size < removedCount) {
                        rebuild(keys.size) // just rebuild the table
                    }
                }
                return removedValue
            }
            pos = pos + 1 and mask
        }
        return nullValue
    }

    override fun clear() {
        if (keys.size > REBUILD_LENGTH_THRESHOLD) {
            initEmptyTable(REBUILD_LENGTH_THRESHOLD)
        } else {
            status.fill(FREE)
            size = 0
            removedCount = 0
        }
    }

    override fun keys(): IntArray {
        val result = IntArray(size)
        var i = 0
        var j = 0
        while (i < keys.size) {
            if (status[i] == FILLED) {
                result[j++] = keys[i]
            }
            i++
        }
        return result
    }

    override fun values(): IntArray {
        val result = IntArray(size)
        var i = 0
        var j = 0
        while (i < values.size) {
            if (status[i] == FILLED) {
                result[j++] = values[i]
            }
            i++
        }
        return result
    }

    override fun iterator(): _Ez_Int__Int_MapIterator {
        return _Ez_Int__Int_HashMapIterator()
    }

    private fun rebuild(newLength: Int) {
        val oldKeys = keys

        val oldValues = values
        val oldStatus = status
        initEmptyTable(newLength)
        for (i in oldKeys.indices) {
            if (oldStatus[i] == FILLED) {
                put(oldKeys[i], oldValues[i])
            }
        }
    }

    private fun initEmptyTable(length: Int) {
        keys = IntArray(length)
        values = IntArray(length)
        status = ByteArray(length)
        size = 0
        removedCount = 0
        mask = length - 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as _Ez_Int__Int_HashMap
        if (size != that.size) {
            return false
        }
        for (i in keys.indices) {
            if (status[i] == FILLED) {
                val   thatValue = that[keys[i]]
                if (thatValue != values[i]) {
                    return false
                }
            }
        }
        return true
    }

    override fun hashCode(): Int {
        val entryHashes = IntArray(size)
        run {
            var i = 0
            var j = 0
            while (i < status.size) {
                if (status[i] == FILLED) {
                    var hash = HASHCODE_INITIAL_VALUE
                    hash =
                        (hash xor keys[i]) * HASHCODE_MULTIPLIER
                    hash =
                        (hash xor values[i]) * HASHCODE_MULTIPLIER
                    entryHashes[j++] = hash
                }
                i++
            }
        }
        entryHashes.sort()
        var hash = HASHCODE_INITIAL_VALUE
        for (i in 0 until size) {
            hash = (hash xor entryHashes[i]) * HASHCODE_MULTIPLIER
        }
        return hash
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('{')
        for (i in keys.indices) {
            if (status[i] == FILLED) {
                if (sb.length > 1) {
                    sb.append(", ")
                }
                sb.append(keys[i])
                sb.append('=')
                sb.append(values[i])
            }
        }
        sb.append('}')
        return sb.toString()
    }

    private inner class _Ez_Int__Int_HashMapIterator : _Ez_Int__Int_MapIterator {
        private var curIndex = 0
        override fun hasNext(): Boolean {
            return curIndex < status.size
        }


        override val key: Int

            get() {
                if (curIndex == keys.size) {
                    throw NoSuchElementException("Iterator doesn't have more entries")
                }
                return keys[curIndex]
            }


        override val value: Int

            get() {
                if (curIndex == values.size) {
                    throw NoSuchElementException("Iterator doesn't have more entries")
                }
                return values[curIndex]
            }

        override fun next() {
            if (curIndex == status.size) {
                return
            }
            curIndex++
            while (curIndex < status.size && status[curIndex] != FILLED) {
                curIndex++
            }
        }

        init {
            while (curIndex < status.size && status[curIndex] != FILLED) {
                curIndex++
            }
        }
    }

    init {
        require(capacity >= 0) { "Capacity must be non-negative" }
        val length = Integer.highestOneBit(4 * max(1, capacity) - 1)
        // Length is a power of 2 now
        initEmptyTable(length)
    }
}

interface _Ez_Int__Int_Map {

    val size: Int

    fun isEmpty(): Boolean

    fun containsKey(
        key: Int
    ): Boolean

    operator fun  get(
        key: Int
    ): Int

    fun  put(
        key: Int,
        value: Int
    ): Int

    fun  remove(
        key: Int
    ): Int

    fun clear()

    fun keys(): IntArray

    fun values(): IntArray

    operator fun iterator(): _Ez_Int__Int_MapIterator

    override fun hashCode(): Int

    override fun toString(): String
}

interface _Ez_Int__Int_MapIterator {
    operator fun hasNext(): Boolean

    val key: Int

    val value: Int

    operator fun next()
}

inline fun IntIntMap.forEach(act: (key: Int, value: Int) -> Unit) {
    val ite = iterator()
    while(ite.hasNext()) {
        act(ite.key, ite.value)
        ite.next()
    }
}