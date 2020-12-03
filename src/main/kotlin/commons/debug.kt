package commons

private var markedNanoTime = 0L
fun markTime() {
    markedNanoTime = System.nanoTime()
}

fun printTime() {
    val ms = (System.nanoTime() - markedNanoTime) divCeil 1000000
    println("Time: ${ms}ms")
}