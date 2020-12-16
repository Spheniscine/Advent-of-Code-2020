package d14

import commons.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random

private val input by lazyInput(14, "gmail")

fun main() {
    println("--- Day 14: Docking Data ---")
    markTime()

    val lines = input.lines()

    val setMaskRegex = Regex("""^mask = (.+)$""")
    val setMemRegex = Regex("""^mem\[(.+)] = (.+)$""")

    var upMask = 0L
    var downMask = 0L
    val mem = LongLongMap()

    for(line in lines) {
        regexWhen<Unit>(line) {
            setMaskRegex then { (maskStr) ->
                upMask = 0L
                downMask = -1L
                for(i in maskStr.indices) {
                    val d = maskStr[maskStr.lastIndex - i]
                    when(d) {
                        '0' -> downMask -= 1L shl i
                        '1' -> upMask += 1L shl i
                    }
                }
            }
            setMemRegex then { (iStr, vStr) ->
                val i = iStr.toLong()
                val v = vStr.toLong()

                mem[i] = v or upMask and downMask
            }
        }
    }

    val ans1 = mem.values().sum()

    println("Part 1: $ans1")
    printTime()

    markTime()

    var floatMask = 0L
    var uncUpMask = 0L
    mem.clear()

    for(line in lines) {
        regexWhen<Unit>(line) {
            setMaskRegex then { (maskStr) ->
                floatMask = 0L
                uncUpMask = 0L
                for(i in maskStr.indices) {
                    val d = maskStr[maskStr.lastIndex - i]
                    when(d) {
                        '1' -> uncUpMask += 1L shl i
                        'X' -> floatMask += 1L shl i
                    }
                }
            }
            setMemRegex then { (iStr, vStr) ->
                val i = iStr.toLong() or uncUpMask
                val v = vStr.toLong()

                upMask = floatMask
                downMask = -1L

                while(true) {
                    mem[i or upMask and downMask] = v

                    if(upMask == 0L) break

                    upMask = upMask - 1 and floatMask
                    downMask = floatMask.inv() or upMask
                }
            }
        }
    }

    val ans2 = mem.values().sum()

    println("Part 2: $ans2")
    printTime()
}

typealias LongLongMap = _Ez_Long__Long_HashMap
inline operator fun LongLongMap.set(key: Long, value: Long) { put(key, value) }
inline operator fun LongLongMap.contains(key: Long) = containsKey(key)

class _Ez_Long__Long_HashMap(capacity: Int = DEFAULT_CAPACITY, val nullValue: Long = DEFAULT_NULL_VALUE) :
    _Ez_Long__Long_Map {
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
        private const  val   DEFAULT_NULL_VALUE = 0L
        private val hashSeed = Random.nextLong()
    }

    private lateinit   var keys: LongArray
    private lateinit   var values: LongArray
    private lateinit var status: ByteArray
    override var size = 0
        private set
    private var removedCount = 0
    private var mask = 0

    constructor(map: _Ez_Long__Long_Map) : this(map.size) {
        val it = map.iterator()
        while (it.hasNext()) {
            put(it.key, it.value)
            it.next()
        }
    }

    constructor(javaMap: Map<Long, Long>) : this(javaMap.size) {
        for ((key, value) in javaMap) {
            put(key, value)
        }
    }

    private fun getStartPos(h: Long): Int {
        var x = (h xor hashSeed) * -7046029254386353131
        x = (x xor (x ushr 30)) * -4658895280553007687
        x = (x xor (x ushr 27)) * -7723592293110705685
        return (x xor (x ushr 31)).toInt() and mask
    }

    override fun isEmpty(): Boolean = size == 0

    override fun containsKey(
        key: Long
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
        key: Long
    ): Long {
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
        key: Long,
        value: Long
    ): Long {
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
        key: Long
    ): Long {
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

    override fun values(): LongArray {
        val result = LongArray(size)
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

    override fun iterator(): _Ez_Long__Long_MapIterator {
        return _Ez_Long__Long_HashMapIterator()
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
        values = LongArray(length)
        status = ByteArray(length)
        size = 0
        removedCount = 0
        mask = length - 1
    }

    fun contentEquals(that: _Ez_Long__Long_HashMap): Boolean {
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

    private inner class _Ez_Long__Long_HashMapIterator : _Ez_Long__Long_MapIterator {
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


        override val value: Long

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

interface _Ez_Long__Long_Map {

    val size: Int

    fun isEmpty(): Boolean

    fun containsKey(
        key: Long
    ): Boolean

    operator fun  get(
        key: Long
    ): Long

    fun  put(
        key: Long,
        value: Long
    ): Long

    fun  remove(
        key: Long
    ): Long

    fun clear()

    fun keys(): LongArray

    fun values(): LongArray

    operator fun iterator(): _Ez_Long__Long_MapIterator

    override fun toString(): String
}

interface _Ez_Long__Long_MapIterator {
    operator fun hasNext(): Boolean

    val key: Long

    val value: Long

    operator fun next()
}

inline fun LongLongMap.forEach(act: (key: Long, value: Long) -> Unit) {
    val ite = iterator()
    while(ite.hasNext()) {
        act(ite.key, ite.value)
        ite.next()
    }
}