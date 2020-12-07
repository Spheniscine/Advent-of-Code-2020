package commons

interface NonNullMap<K, V: Any>: Map<K, V> {
    override fun get(key: K): V
}

interface NonNullMutableMap<K, V: Any>: MutableMap<K, V>, NonNullMap<K, V> {
    operator fun set(key: K, value: V) { put(key, value) }
}

@JvmName("defaultMutable")
fun <K, V: Any> MutableMap<K, V>.default(defaultValue: V): NonNullMutableMap<K, V> {
    val map = this
    return object : NonNullMutableMap<K, V>, MutableMap<K, V> by map {
        override fun get(key: K): V = map[key] ?: defaultValue
        override fun put(key: K, value: V): V? = if(value == defaultValue) remove(key) else map.put(key, value)
    }
}

fun <K, V: Any> MutableMap<K, V>.memoize(defaultValue: (K) -> V): NonNullMap<K, V> = autoPut(defaultValue)

fun <K, V: Any> MutableMap<K, V>.autoPut(defaultValue: (K) -> V): NonNullMutableMap<K, V> {
    val map = this
    return object : NonNullMutableMap<K, V>, MutableMap<K, V> by map {
        override fun get(key: K): V = map.getOrPut(key) { defaultValue(key) }
    }
}

fun <K, V : Any> Map<K, V>.default(defaultValue: V): NonNullMap<K, V> {
    val map = this
    return object : NonNullMap<K, V>, Map<K, V> by map {
        override fun get(key: K): V = map[key] ?: defaultValue
    }
}

fun <K, V : Any> Map<K, V>.nonNull(): NonNullMap<K, V> {
    val map = this
    return object : NonNullMap<K, V>, Map<K, V> by map {
        override fun get(key: K): V = map.getValue(key)
    }
}

@JvmName("nonNullMutable")
fun <K, V : Any> MutableMap<K, V>.nonNull(): NonNullMutableMap<K, V> {
    val map = this
    return object : NonNullMutableMap<K, V>, MutableMap<K, V> by map {
        override fun get(key: K): V = map.getValue(key)
    }
}

fun <K, V : Any> Map<K, V>.default(defaultValue: (K) -> V): NonNullMap<K, V> {
    val map = this
    return object : NonNullMap<K, V>, Map<K, V> by map {
        override fun get(key: K): V = map[key] ?: defaultValue(key)
    }
}

@JvmName("defaultMutable")
fun <K, V: Any> MutableMap<K, V>.default(defaultValue: (K) -> V): NonNullMutableMap<K, V> {
    val map = this
    return object : NonNullMutableMap<K, V>, MutableMap<K, V> by map {
        override fun get(key: K): V = map[key] ?: defaultValue(key)
    }
}