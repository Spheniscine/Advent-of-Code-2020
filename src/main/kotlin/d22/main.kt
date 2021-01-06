package d22

import commons.*
import java.io.File

private val input by lazyInput(22, "gmail")

fun main() {
    println("--- Day 22: Crab Combat ---")
    markTime()

    val playerDecks = input.split("\n\n").map {
        it.split("\n").drop(1).map { it.toInt() }
    }

    run {
        val players = playerDecks.map { it.toCollection(ArrayDeque()) }

        while (players.all { it.isNotEmpty() }) {
            val roundWinner = players.maxByOrNull { it.first() }!!
            val roundLoser = players.first { it !== roundWinner }

            val a = roundWinner.removeFirst()
            val b = roundLoser.removeFirst()
            roundWinner.add(a)
            roundWinner.add(b)
        }

        val winner = players.first { it.isNotEmpty() }

        val ans1 = winner.indices.sumOf { i -> winner[i] * (winner.size - i) }

        println("Part 1: $ans1")
        printTime()
    }

    markTime()

    val players = playerDecks.map { it.toCollection(ArrayDeque()) }

    fun List<ArrayDeque<Int>>.recurse(): Int {
        val states = HashSet<Long>()

        while (this.all { it.isNotEmpty() }) {
            val state = with(sipHasher) {
                init()
                for(player in this@recurse) {
                    for(card in player) acc(card)
                    comma()
                }
                finishLong()
            }

            if(!states.add(state))
                return 0

            val roundWinner = if(this.all { it.first() < it.size }) {
                val newDecks = map { player ->
                    val deck = ArrayDeque<Int>()
                    for(i in 1..player.first()) deck.add(player[i])
                    deck
                }

                this[newDecks.recurse()]
            } else this.maxByOrNull { it.first() }!!
            val roundLoser = this.first { it !== roundWinner }

            val a = roundWinner.removeFirst()
            val b = roundLoser.removeFirst()
            roundWinner.add(a)
            roundWinner.add(b)
        }

        val winner = indices.first { this[it].isNotEmpty() }
        return winner
    }

    val winner = players[players.recurse()]

    val ans2 = winner.indices.sumOf { i -> winner[i] * (winner.size - i) }

    println("Part 2: $ans2")
    printTime()
}