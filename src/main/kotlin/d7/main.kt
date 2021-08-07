package d7

import commons.*
import java.io.File

private val input by lazyInput(7, "gmail")

fun main() {
    println("--- Day 7: Handy Haversacks ---")
    markTime()

    val bags = StringHashMap<Bag>().autoPut { Bag(it) }
    val contentRegex = Regex("""(\d+) (.+?) bags?[,.]""")

    for(line in input.lines()) {
        val (currColor, contentStr) = line.split(" bags contain ")
        val curr = bags[currColor]

        for(match in contentRegex.findAll(contentStr)) {
            val (numStr, contentColor) = match.destructured
            val content = bags[contentColor]
            curr.contains.add(Quantity(content, numStr.toInt()))
            content.parents.add(curr)
        }
    }

    val root = bags["shiny gold"]
    val ans1 = run ans@ {
        val vis = HashSet<Bag>()
        val stk = mutableListOf(root)

        while(stk.isNotEmpty()) {
            val curr = stk.removeLast()

            for(parent in curr.parents) {
                if(vis.add(parent)) stk.add(parent)
            }
        }

        vis.size
    }

    println("Part 1: $ans1")
    printTime()

    markTime()

    val ans2 = root.weight-1

    println("Part 2: $ans2")
    printTime()
}

data class Quantity(val bag: Bag, val num: Int)

class Bag(val color: String) {
    val contains = mutableListOf<Quantity>()
    val parents = mutableListOf<Bag>()

    var _weight = 0L
    val weight: Long get() {
        if(_weight == 0L) {
            _weight = 1L + contains.sumOf { (child, num) -> child.weight * num }
        }
        return _weight
    }
}