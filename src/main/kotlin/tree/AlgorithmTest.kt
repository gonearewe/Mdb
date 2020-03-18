package tree

import java.io.StringReader

fun main() {
    val database = Algorithm.newBPlusTree()
    val ops = listOf(Algorithm::insert, Algorithm::search, Algorithm::delete)

    val cmds = StringReader(testCases).readLines().map { it.trim() }
    cmds.forEach nextRound@{
        if (it.isBlank()) {
            return@nextRound
        }

        val tokens = it.split(regex = ":?\\s".toRegex())
        when (tokens[0]) {
            "insert" -> {
                val res = database.insert(tokens[1].toInt() to tokens[2])
                verify(res.toString(), tokens[3], "$it\nDatabase Snapshot:\n${database.snapshot()}")
            }
            "delete" -> {
                val res = database.delete(tokens[1].toInt())
                verify(res.toString(), tokens[2], "$it\nDatabase Snapshot:\n${database.snapshot()}")
            }
            "search" -> {
                val res = database.search(tokens[1].toInt())
                verify(res?.second ?: "null", tokens[2], "$it\nDatabase Snapshot:\n${database.snapshot()}")
            }
            else -> {
                println("unexpected cmd: ${tokens[0]}")
                System.exit(-1)
            }
        }
    }

    println()
    println("TESTS PASSED")
}

private fun verify(output: String, expect: String, helpinfo: String) {
    if (output != expect) {
        println("FAIL: expecting: $expect found: $output")
        println("INFO: $helpinfo")
        System.exit(-1)
    }
}

const val testCases = """
    insert 1 First: true
    insert 2 Second: true
    insert 3 Third: true
    insert 4 Fourth: true
    insert 5 Fifth: true
    insert 6 Sixth: true
    insert 7 Seventh: true
    
    search 4: Fourth
    search 7: Seventh
    search 1: First
    
    insert 2 Again: false
    insert 7 Seventh: false
    search 0: null
    
    delete 4: true
    search 4: null
    insert 4 NewFourth: true
    search 4: NewFourth
    
    delete 0: false
    search 9: null
    insert 10 New: true
    search 10: New
    
    insert 12 One12: true
    insert 14 One14: true
    insert 15 One15: true
    insert 17 One17: true
    insert 20 One20: true
    insert 13 One13: true
    insert 18 One18: true
    insert 21 One21: true
    insert 28 One28: true
    insert 16 One16: true
    delete 16: true
    delete 28: true
    delete 21: true
    delete 18: true
    delete 13: true
    delete 20: true
    delete 17: true
    delete 12: true
    search 15: One15
    search 1: First
    search 3: Third
    search 7: Seventh
"""