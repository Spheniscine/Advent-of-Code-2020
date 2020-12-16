package d16

import commons.*
import java.io.File
import java.util.Collections
import java.util.IdentityHashMap

private val input by lazyInput(16, "gmail")

fun main() {
    println("--- Day 16: Ticket Translation ---")
    markTime()

    val (fieldsIn, myTicketIn, nearTicketsIn) = input.split("\n\n")

    val fieldRegex = Regex("""(.+): (\d+)-(\d+) or (\d+)-(\d+)""")
    val fields = fieldsIn.lines().map {
        val l = fieldRegex.capture(it)!!.toList()
        var i = 0
        Field(l[i++], l[i++].toInt(), l[i++].toInt(), l[i++].toInt(), l[i].toInt())
    }

    val nearTickets = nearTicketsIn.lineSequence().drop(1).map {
        it.split(',').map { it.toInt() }
    }.toList()

    val myTicket = myTicketIn.lines()[1].split(',').map { it.toInt() }
    val validTickets = mutableListOf(myTicket)

    val ans1 = nearTickets.sumOf { ticket ->
        var ok = true
        ticket.sumOf { value ->
            if(fields.none { value in it }) {
                ok = false
                value
            } else 0
        }.also { if(ok) validTickets.add(ticket) }
    }

    println("Part 1: $ans1")
    printTime()

    markTime()

    val possibilities = List(fields.size) { i ->
        fields.filter { field ->
            validTickets.all { ticket ->
                ticket[i] in field
            }
        }
    }

    var ans2 = 1L
    val finalFields = MutableList(fields.size) { fields[0] }
    val seen = IdentityHashSet<Field>()
    for(i in possibilities.indices.sortedBy { possibilities[it].size }) {
        val res = possibilities[i].find { it !in seen }!!
        seen.add(res)
        finalFields[i] = res
        if(res.name.startsWith("departure")) ans2 *= myTicket[i]
    }

    println("Part 2: $ans2")
    printTime()
}

data class Field(val name: String, val start1: Int, val end1: Int, val start2: Int, val end2: Int) {
    operator fun contains(value: Int) = value in start1..end1 || value in start2..end2
}