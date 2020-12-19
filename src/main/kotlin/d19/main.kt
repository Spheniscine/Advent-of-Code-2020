package d19

import commons.*

private val input by lazyInput(19, "gmail")

fun main() {
    println("--- Day 19: Monster Messages ---")
    markTime()

    val (rulesStr, messagesStr) = input.split("\n\n")

    val rulesLines = rulesStr.lines()
    val solver = Solver(rulesLines)

    val messages = messagesStr.lines()
    val ans1 = solver.countMatches(messages)

    println("Part 1: $ans1")
    printTime()

    markTime()

    solver.run {
        val r42 = rules[42]
        val r31 = rules[31]

        rules[8] = object: Rule {
            override fun partialMatch(str: String, start: Int): Set<Int> {
                val matches = BooleanArray(str.length - start + 1)
                matches[0] = true
                for(i in 0 until matches.lastIndex) if(matches[i]) {
                    for(j in r42.partialMatch(str, i+start)) {
                        matches[j - start] = true
                    }
                }

                val ans = mutableSetOf<Int>()
                for(i in 1..matches.lastIndex) if(matches[i]) ans.add(i + start)
                return ans
            }
        }

        rules[11] = object: Rule {
            override fun partialMatch(str: String, start: Int): Set<Int> {
                val m42 = mutableListOf(setOf(start))

                while(true) {
                    val next = mutableSetOf<Int>()

                    for(i in m42.last()) {
                        next.addAll(r42.partialMatch(str, i))
                    }

                    if(next.isEmpty()) break
                    m42.add(next)
                }

                val ans = mutableSetOf<Int>()
                i@ for(i in 1..m42.lastIndex) {
                    var t = m42[i]
                    for(j in 0 until i) {
                        val next = mutableSetOf<Int>()
                        for(s in t) {
                            next.addAll(r31.partialMatch(str, s))
                        }
                        if(next.isEmpty()) continue@i
                        t = next
                    }
                    ans.addAll(t)
                }

                return ans
            }
        }
    }

    val ans2 = solver.countMatches(messages)

    println("Part 2: $ans2")
    printTime()
}



class Solver(lines: List<String>) {
    val rules = mutableListOf<Rule>()

    inner class RuleRef(val id: Int): Rule {
        override fun partialMatch(str: String, start: Int) = rules[id].partialMatch(str, start)
    }

    fun String.parseRule(): Rule {
        var split = split(" | ")
        if(split.size > 1) return OrRule(split.map { it.parseRule() })
        split = split(' ')
        if(split.size > 1) return ConcatRule(split.map { it.parseRule() })

        return if(this[0] == '"') StrRule(removeSurrounding("\""))
        else RuleRef(toInt())
    }

    fun countMatches(messages: List<String>) = messages.count { message ->
        message.length in rules[0].partialMatch(message, 0)
    }

    init {
        for(line in lines) {
            val (idStr, ruleStr) = line.split(": ")
            val id = idStr.toInt()

            while(id > rules.lastIndex) rules.add(Rule.empty)

            rules[id] = ruleStr.parseRule()
        }
    }
}

interface Rule {
    companion object {
        val empty = StrRule("")
    }
    fun partialMatch(str: String, start: Int): Set<Int>
}

class StrRule(val toMatch: String): Rule {
    override fun partialMatch(str: String, start: Int): Set<Int> {
        val end = start + toMatch.length
        if(end <= str.length) {
            for(i in start until end) {
                if(str[i] != toMatch[i - start]) return emptySet()
            }
            return setOf(end)
        }

        return emptySet()
    }
}

class ConcatRule(val rules: List<Rule>): Rule {
    override fun partialMatch(str: String, start: Int): Set<Int> {
        var end = setOf(start)
        for(rule in rules) {
            val new = mutableSetOf<Int>()
            for(st in end) {
                new.addAll(rule.partialMatch(str, st))
            }
            end = new
        }
        return end
    }
}

class OrRule(val rules: List<Rule>): Rule {
    override fun partialMatch(str: String, start: Int): Set<Int> {
        val res = mutableSetOf<Int>()

        for(rule in rules) {
            res.addAll(rule.partialMatch(str, start))
        }

        return res
    }
}