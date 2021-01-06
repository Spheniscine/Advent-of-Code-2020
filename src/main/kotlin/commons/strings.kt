package commons

fun CharSequence.translate(from: CharSequence, to: CharSequence): String =
    String(CharArray(length) {
        val i = from.indexOf(this[it])
        if(i == -1) this[it] else to[i]
    })

abstract class AbstractCharSequence: CharSequence {
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = _subSequence(startIndex, endIndex)

    override fun toString() = String(CharArray(length, ::get))
}
fun CharSequence._subSequence(startIndex_: Int, endIndex_: Int): CharSequence {
    require(endIndex_ in startIndex_..length) {
        "endIndex $endIndex_ either less than startIndex $startIndex_ or greater than CharSequence length $length"
    }
    return object: AbstractCharSequence() {
        override val length: Int
            get() = endIndex_ - startIndex_

        override fun get(index: Int): Char = this@_subSequence[index + startIndex_]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            this@_subSequence._subSequence(startIndex_ + startIndex, startIndex_ + endIndex)
    }
}

fun CharArray.asCharSequence(): CharSequence = object: AbstractCharSequence() {
    override val length: Int get() = this@asCharSequence.size
    override fun get(index: Int): Char = this@asCharSequence[index]
}