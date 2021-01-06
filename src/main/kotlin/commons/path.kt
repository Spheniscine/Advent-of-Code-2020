package commons

import java.io.File

fun lazyInput(day: Int, type: String) = lazyFile(day, "input/$type.in")
fun lazyFile(day: Int, name: String) = lazy { File("src/main/kotlin/d$day/$name").readText() }