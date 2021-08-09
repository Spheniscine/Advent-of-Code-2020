package d9

import commons.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random

private val input by lazyInput(9, "gmail")

fun main() {
    println("--- Day 9: Encoding Error ---")
    markTime()
    
    val lines = input.lines()
    val n = lines.size
    val A = LongArray(n) { lines[it].toLong() }

    val ans1 = run ans@{
        val cnt = LongIntMap()
        for (i in 1 until preambleLength) {
            for (j in i - 1 downTo 0) {
                cnt[A[i] + A[j]]++
            }
        }

        for (i in preambleLength until n) {
            var a = A[i]
            if(cnt[a] == 0)
                return@ans a

            for(j in i - preambleLength + 1 until i) {
                val b = a + A[j]
                cnt[b]++
            }

            a = A[i - preambleLength]
            for(j in i - preambleLength + 1 until i) {
                val b = a + A[j]
                val c = cnt[b]
                if(c == 1) cnt.remove(b) else cnt[b] = c-1
            }
        }

        0L
    }

    println("Part 1: $ans1")
    printTime()

    markTime()

    val ans2 = run ans@ {
        val P = LongIntMap(nullValue = -1)
        P[0] = 0

        var prefixSum = 0L
        for(i in 0 until n) {
            prefixSum += A[i]

            val target = prefixSum - ans1
            val j = P[target]
            if(j != -1 && j != i) {
                return@ans (j..i).minOf { A[it] } + (j..i).maxOf { A[it] }
            }

            P[prefixSum] = i+1
        }

        -1L
    }

    println("Part 2: $ans2")
    printTime()
}

const val preambleLength = 25

typealias LongIntMap = _Ez_Long__Int_HashMap
inline operator fun LongIntMap.set(key: Long, value: Int) { put(key, value) }
inline operator fun LongIntMap.contains(key: Long) = containsKey(key)

class _Ez_Long__Int_HashMap(capacity: Int = DEFAULT_CAPACITY, val nullValue: Int = 0) :
    _Ez_Long__Int_Map {
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
        private val hashSeed = Random.nextLong()
        private val gamma = Random.nextLong() or 1
    }

    private fun getStartPos(h: Long): Int {
        var x = h * gamma + hashSeed
        x = (x xor (x ushr 30)) * -4658895280553007687
        x = (x xor (x ushr 27)) * -7723592293110705685
        return (x xor (x ushr 31)).toInt() and mask
    }

    private lateinit   var keys: LongArray
    private lateinit   var values: IntArray
    private lateinit var status: ByteArray
    override var size = 0
        private set
    private var removedCount = 0
    private var mask = 0

    constructor(map: _Ez_Long__Int_Map) : this(map.size) {
        val it = map.iterator()
        while (it.hasNext()) {
            put(it.key, it.value)
            it.next()
        }
    }

    constructor(javaMap: Map<Long, Int>) : this(javaMap.size) {
        for ((key, value) in javaMap) {
            put(key, value)
        }
    }

    override fun isEmpty(): Boolean = size == 0

    override fun containsKey(
        key: Long
    ): Boolean {
        var pos = getStartPos(key)
        var step = 0
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                return true
            }
            pos = pos + ++step and mask
        }
        return false
    }

    override fun  get(
        key: Long
    ): Int {
        var pos = getStartPos(key)
        var step = 0
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                return values[pos]
            }
            pos = pos + ++step and mask
        }
        return nullValue
    }

    override fun  put(
        key: Long,
        value: Int
    ): Int {
        var pos = getStartPos(key)
        var step = 0
        while (status[pos] == FILLED) {
            if (keys[pos] == key) {
                val   oldValue = values[pos]
                values[pos] = value
                return oldValue
            }
            pos = pos + ++step and mask
        }
        if (status[pos] == FREE) {
            status[pos] = FILLED
            keys[pos] = key
            values[pos] = value
            size++
            if ((size + removedCount) * 2 > keys.size) {
                if(size > removedCount) rebuild(keys.size * 2) // enlarge the table
                else rebuild(keys.size)
            }
            return nullValue
        }
        val removedPos = pos
        pos = pos + ++step and mask
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                val   oldValue = values[pos]
                values[pos] = value
                return oldValue
            }
            pos = pos + ++step and mask
        }
        status[removedPos] = FILLED
        keys[removedPos] = key
        values[removedPos] = value
        size++
        removedCount--
        return nullValue
    }

    override fun  remove(
        key: Long
    ): Int {
        var pos = getStartPos(key)
        var step = 0
        while (status[pos] != FREE) {
            if (status[pos] == FILLED && keys[pos] == key) {
                val   removedValue = values[pos]
                status[pos] = REMOVED
                size--
                removedCount++
                if (keys.size > REBUILD_LENGTH_THRESHOLD) {
                    if (8 * size <= keys.size) {
                        rebuild(keys.size / 2) // compress the table
                    } // else if (size < removedCount) {
//                        rebuild(keys.size) // just rebuild the table
//                    }
                }
                return removedValue
            }
            pos = pos + ++step and mask
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

    override fun keys(): LongArray {
        val result = LongArray(size)
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

    override fun iterator(): _Ez_Long__Int_MapIterator {
        return _Ez_Long__Int_HashMapIterator()
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
        keys = LongArray(length)
        values = IntArray(length)
        status = ByteArray(length)
        size = 0
        removedCount = 0
        mask = length - 1
    }

    fun contentEquals(that: _Ez_Long__Int_HashMap): Boolean {
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

    private inner class _Ez_Long__Int_HashMapIterator : _Ez_Long__Int_MapIterator {
        private var curIndex = 0
        override fun hasNext(): Boolean {
            return curIndex < status.size
        }


        override val key: Long

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

interface _Ez_Long__Int_Map {

    val size: Int

    fun isEmpty(): Boolean

    fun containsKey(
        key: Long
    ): Boolean

    operator fun  get(
        key: Long
    ): Int

    fun  put(
        key: Long,
        value: Int
    ): Int

    fun  remove(
        key: Long
    ): Int

    fun clear()

    fun keys(): LongArray

    fun values(): IntArray

    operator fun iterator(): _Ez_Long__Int_MapIterator

    override fun toString(): String
}

interface _Ez_Long__Int_MapIterator {
    operator fun hasNext(): Boolean

    val key: Long

    val value: Int

    operator fun next()
}

inline fun LongIntMap.forEach(act: (key: Long, value: Int) -> Unit) {
    val ite = iterator()
    while(ite.hasNext()) {
        act(ite.key, ite.value)
        ite.next()
    }
}