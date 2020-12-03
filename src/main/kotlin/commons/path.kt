package commons

import java.io.File

fun lazyInput(day: Int, type: String) = lazy { File("src/main/kotlin/d$day/input/$type.in").readText() }