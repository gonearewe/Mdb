package Tree

import java.util.*
import kotlin.collections.ArrayList

class Algorithm

interface BPlusTreeNode {
    val id: String
    val order get() = 9

    fun search(key: Int): Pair<Int, String>?
}

class BPlusInternalTreeNode : BPlusTreeNode {
    companion object {
        var cnt = 0
    }

    override val id: String
    val keys = LinkedList<Int>()
    val children = LinkedList<BPlusTreeNode>()

    init {
        id = "I_$cnt"
        cnt++
    }

    override fun toString(): String {
        return buildString {
            append("id=$id,")
            append("keys= $keys, ")
            append("children= ")
            children.forEach {
                append(it.id)
                append(", ")
            }

            append("\n")
            children.forEach {
                append(it.toString())
            }
        }
    }

    override fun search(key: Int): Pair<Int, String>? {
        return locateChild(key)?.search(key)
    }

    private fun locateChild(key: Int): BPlusTreeNode? {
        for (index in 0 until keys.size) {
            if (keys[index] >= key) {
                return children[index + 1]
            }
        }

        return null
    }
}

class BPlusLeafNode() : BPlusTreeNode {
    companion object {
        var cnt = 0
    }

    override val id: String
    val records = ArrayList<Pair<Int, String>>()
    val nextLeaf: BPlusLeafNode? = null

    init {
        id = "L_${cnt}"
        cnt++
    }

    override fun search(key: Int): Pair<Int, String>? {
        val position = records.binarySearch(comparison = { it.first })
        if (position < 0) {
            return null
        }

        return records[position]
    }

    override fun toString(): String {
        return buildString {
            append("id=$id,")
            append("records= ")
            records.forEach {
                append(it.toString())
                append(", ")
            }
            append("\n")
        }
    }
}

fun main() {
}