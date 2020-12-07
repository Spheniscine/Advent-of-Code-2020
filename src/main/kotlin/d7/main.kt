package d7

import commons.*
import java.io.File

private val input by lazyInput(7, "gmail")

fun main() {
    println("--- Day 7: Handy Haversacks ---")
    markTime()

    val bags = HashMap<String, Bag>().autoPut { Bag(it) }
    val contentRegex = Regex("""(\d+) (.+?) bags?[,.]""")

    for(line in input.lines()) {
        val (currColor, contentStr) = line.split(" bags contain ")
        val curr = bags[currColor]

        for(match in contentRegex.findAll(contentStr)) {
            val (numStr, contentColor) = match.destructured
            val content = bags[contentColor]
            curr.contains[content] = numStr.toInt()
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

    val vis = HashSet<Bag>()
    val stk = mutableListOf(root)

    while(stk.isNotEmpty()) {
        val curr = stk.removeLast()

        if(vis.add(curr)) {
            stk.add(curr)
            for(child in curr.contains.keys) stk.add(child)
        } else {
            curr.weight = 1
            for((child, num) in curr.contains) {
                curr.weight += child.weight * num
            }
        }
    }

    val ans2 = root.weight-1

    println("Part 2: $ans2")
    printTime()
}

class Bag(val color: String) {
    val contains = mutableMapOf<Bag, Int>()
    val parents = mutableListOf<Bag>()

    var weight = 0L
}