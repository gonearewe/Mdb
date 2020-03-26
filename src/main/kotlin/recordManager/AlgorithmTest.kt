package recordManager

import java.io.File

fun main() {
    val database = Algorithm.newBPlusTree()
    val ops = listOf(Algorithm::insert, Algorithm::search, Algorithm::delete)

    var cmds = File("src/test/resources/algorithm.txt").readLines().map { it.trim() }
    cmds = cmds.filter { !it.startsWith("#") && it.isNotBlank() }

    cmds.forEach {
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
        println("INFO: \n$helpinfo")
        System.exit(-1)
    }
}