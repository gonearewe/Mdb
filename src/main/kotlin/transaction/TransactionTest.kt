package transaction

import database.Database
import pager.MmapPager
import pager.Pager
import recordManager.DatabaseRecord
import recordManager.RecordManager
import recordManager.Tree
import java.io.File

fun main() {
    val lines = File("src/main/kotlin/transaction/TransactionTest.kt")
            .readLines().map { it.trim() }.filter { !it.startsWith("#") || it.isNotBlank() }
    var i = 0
    while (i < lines.size) {
        val cases = ArrayList<String>()
        i++ // skip "COMMANDS"
        while (lines[i] != "EXPECTATIONS") {
            cases.add(lines[i])
            i++
        }

        val expectations = ArrayList<DatabaseRecord>()
        i++ // skip "EXPECTATIONS"
        while (i < lines.size && lines[i] != "COMMANDS") {
            val r = lines[i].split(" ")
            expectations.add(DatabaseRecord(r[0].toInt(), r[1]))
            i++
        }

        startTests(cases, expectations)
    }

}

fun startTests(cases: List<String>, expectations: List<DatabaseRecord>) {
    val pager: Pager = MmapPager.of(".\\test.db")!!
    val recordManager: RecordManager = Tree(pager)
    val db = Database(recordManager)

}