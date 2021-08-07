package d21

import commons.*

private val input by lazyInput(21, "gmail")

fun main() {
    println("--- Day 21: Allergen Assessment ---")
    markTime()

    val foods = input.lines().map { ln ->
        val split = ln.split(" (contains ")
        val ingredients = split[0].split(' ')
        val allergens = split[1].dropLast(1).split(", ")

        Food(ingredients, allergens)
    }

    require(foods.size <= 64) { "too many foods for bitfield" }

    val ingredientMasks = StringHashMap<Long>().default(0L)
    val allergenMasks = StringHashMap<Long>().default(0L)

    for(i in foods.indices) {
        for(e in foods[i].ingredients) {
            ingredientMasks[e] += 1L shl i
        }
        for(e in foods[i].allergens) {
            allergenMasks[e] += 1L shl i
        }
    }

    val allergenIngredients = mutableListOf<String>()

    val ans1 = ingredientMasks.entries.sumOf { (ings, ingm) ->
        val isAllergen = allergenMasks.values.any { allm ->
            ingm or allm == ingm
        }

        if(!isAllergen) ingm.countOneBits() else {
            allergenIngredients.add(ings)
            0
        }
    }

    println("Part 1: $ans1")
    printTime()

    markTime()


    val poss = allergenIngredients.associateWithTo(StringHashMap()) { ing ->
        val ingm = ingredientMasks[ing]
        val res = StringHashSet()
        for((alls, allm) in allergenMasks) {
            if(ingm or allm == ingm) res.add(alls)
        }

        res
    }

    val dangerous = mutableListOf<Pair<String, String>>()

    while(poss.isNotEmpty()) {
        val (key, value) = poss.entries.first { it.value.size == 1 }
        poss.remove(key)
        val allergen = value.first()
        dangerous.add(key to allergen)

        for(v in poss.values) v.remove(allergen)
    }

    dangerous.sortBy { it.second }

    val ans2 = dangerous.joinToString(",") { it.first }

    println("Part 2: $ans2")
    printTime()
}

data class Food(val ingredients: List<String>, val allergens: List<String>)