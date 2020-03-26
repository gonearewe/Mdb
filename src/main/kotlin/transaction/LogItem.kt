package transaction

interface LogItem

internal data class Begin(val txID: Int) : LogItem {
    override fun toString(): String {
        return "BEGIN $txID"
    }
}

internal data class Commit(val txID: Int) : LogItem {
    override fun toString(): String {
        return "COMMIT $txID"
    }
}

internal data class Insert(val txID: Int, val recordKey: Int, val recordValue: String) : LogItem {
    override fun toString(): String {
        return "insert $txID $recordKey $recordValue"
    }
}

internal data class Delete(val txID: Int, val recordKey: Int, val recordValue: String) : LogItem {
    override fun toString(): String {
        return "delete $txID $recordKey $recordValue"
    }
}

internal data class Set(val txID: Int, val recordKey: Int, val recordOldValue: String, val recordNewValue: String) : LogItem {
    override fun toString(): String {
        return "set $txID $recordKey $recordOldValue $recordNewValue"
    }
}

internal data class StartCheckPoint(val txIDs: List<Int>) : LogItem {
    override fun toString(): String {
        var s = ""
        txIDs.forEach {
            s += " $it"
        }
        return "StartCheckPoint$s"
    }
}

internal class EndCheckPoint() : LogItem {
    override fun toString(): String {
        return "EndCheckPoint"
    }
}

internal data class Abort(val txID: Int) : LogItem {
    override fun toString(): String {
        return "ABORT $txID"
    }
}