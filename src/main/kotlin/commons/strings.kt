package commons

fun String.translate(from: String, to: String) = String(CharArray(length) { i ->
    val j = from.indexOf(this[i])
    if(j >= 0) to[j] else this[i]
})
