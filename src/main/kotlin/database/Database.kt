package database

import pager.MmapPager
import recordManager.DatabaseRecord
import recordManager.RecordManager
import recordManager.Tree
import transaction.Transaction

class Database {
    val recordManager: RecordManager = Tree(MmapPager.of("/tmp")!!)

    fun search(key: Int): DatabaseRecord? = recordManager.search(key)
    fun insert(record: DatabaseRecord): Boolean = recordManager.insert(record)
    fun delete(key: Int): Boolean = recordManager.delete(key)
    fun set(record: DatabaseRecord): Boolean = recordManager.set(record)

    fun update(fn: (tx: Transaction) -> Boolean): Boolean {
        val tx = Transaction(this)
        tx.begin()

        if (!fn(tx)) {
            tx.rollBack()
            return false
        }

        tx.commit()
        return true
    }

    fun flush() {

    }
}