@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(ExperimentalTypeInference::class)
package commons

import kotlin.experimental.ExperimentalTypeInference

fun Regex.capture(string: String) = find(string)?.destructured


inline fun <R> get(@BuilderInference block: GetScope<R>.() -> R): R = GetScope<R>().invoke(block)

open class GetScope<in R> {
    class Result @Deprecated("Use yield function") constructor(val scope: GetScope<*>, val data: Any?):
        Throwable("Uncaught call to GetScope.yield", null, true, false)
    @Suppress("DEPRECATION")
    fun yield(result: R): Nothing { throw Result(this, result) }
}

inline fun GetScope<Unit>.yield(): Nothing = yield(Unit)

inline operator fun <R, G: GetScope<R>> G.invoke(block: G.() -> R): R =
    try { block() }
    catch (r: GetScope.Result) {
        if(r.scope !== this) throw r
        @Suppress("UNCHECKED_CAST")
        r.data as R
    }

inline fun <R> regexWhen(string: String, @BuilderInference block: RegexWhen<R>.() -> R): R = RegexWhen<R>(string).invoke(block)

class RegexWhen<in R>(val regexWhenArg: String): GetScope<R>() {
    inline infix fun Regex.then(block: (MatchResult.Destructured) -> R) { capture(regexWhenArg)?.let { yield(block(it)) } }
    fun error(): Nothing = error("Unparsed string: $regexWhenArg")
}