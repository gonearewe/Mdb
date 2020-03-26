package recordManager

interface RecordManager {
    fun search(key: Int): DatabaseRecord?
    fun insert(record: DatabaseRecord): Boolean
    fun delete(key: Int): Boolean
    fun set(record: DatabaseRecord): Boolean
}