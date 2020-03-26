package transaction

import database.Database
import recordManager.DatabaseRecord

class Transaction(val db: Database) {
    val logs = ArrayList<LogItem>()
    val txID = 999

    fun begin() {
        logs.add(Begin(txID))
    }

    fun rollBack() {
        logs.reversed().forEach {
            when (it) {
                is Insert ->
                    db.delete(it.recordKey)
                is Delete ->
                    db.insert(DatabaseRecord(it.recordKey, it.recordValue))
                is Set ->
                    db.set(DatabaseRecord(it.recordKey, it.recordOldValue))
            }
        }
        logs.add(Abort(txID))
    }

    fun commit() {
        logs.add(Commit(txID))
        db.flush()
    }

    fun search(key: Int): DatabaseRecord? = db.search(key)

    fun insert(record: DatabaseRecord): Boolean {
        logs.add(Insert(txID, record.key, record.value))
        return db.insert(record)
    }

    fun delete(key: Int): Boolean {
        logs.add(Delete(txID, key, db.search(key)!!.value))
        return db.delete(key)
    }

    fun set(record: DatabaseRecord): Boolean {
        logs.add(Set(
                txID,
                record.key,
                recordOldValue = db.search(record.key)!!.value,
                recordNewValue = record.value
        ))
        return db.set(record)
    }
}