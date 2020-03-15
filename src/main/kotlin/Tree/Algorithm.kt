package Tree

import java.util.*

class Algorithm

interface BPlusTreeNode {
    val id: String

    // an internal node has k children and k-1 keys, order/2 <= k <= order
    val order get() = 9

    fun search(key: Int): Pair<Int, String>?
    fun insert(record: Pair<Int, String>): Boolean

    // delete deletes a record according to the given key and
    // returns the deleted record or null if the record is not found.
    fun delete(key: Int): Pair<Int, String>?

    // helper methods for insert

    fun isFull(): Boolean

    // splitSelf splits current node to two nodes and returns them along
    // with one key which is greater than all keys in the left child and
    // no greater than right ones.
    fun splitSelf(splitPoint: Int): Triple<Int, BPlusTreeNode, BPlusTreeNode>

    // helper methods for delete

    // isHalfEmpty tells if the node only has the least number of children
    // required, in which case, one more child deleted will lead to the loss of balance.
    fun isHalfEmpty(): Boolean
}

class BPlusInternalTreeNode(
        val keys: LinkedList<Int> = LinkedList<Int>(),
        val children: LinkedList<BPlusTreeNode> = LinkedList<BPlusTreeNode>()
) : BPlusTreeNode {
    companion object {
        var cnt = 0
    }

    override val id: String

    init {
        id = "I_$cnt"
        cnt++
    }

    override fun search(key: Int): Pair<Int, String>? {
        return children[locateChildIndex(key)].search(key)
    }

    override fun insert(record: Pair<Int, String>): Boolean {
        val index = locateChildIndex(record.first)
        val child = children[index]
        if (!child.isFull()) { // it's a simple case
            return child.insert(record)
        }

        splitChildAt(index)
        // now children[index] is a new child (left part of the old one)
        // and so is children[index+1] (right part of the old one)

        // in fact, we only need to check the two new keys, but whatever
        val newIndex = locateChildIndex(record.first)
        return children[newIndex].insert(record) // now it's apparently not full, just insert
    }

    override fun delete(key: Int): Pair<Int, String>? {
        TODO("Not yet implemented")
    }

    override fun isFull(): Boolean {
        return children.size == order
    }

    // locateChildIndex queries current node's keys to tell which child the key belongs to.
    private fun locateChildIndex(key: Int): Int {
        for (index in 0 until keys.size) {
            if (keys[index] >= key) {
                return index + 1
            }
        }

        return keys.size - 1
    }

    private fun splitChildAt(index: Int) {
        val splitPoint = order / 2 // it belongs to the new right child
        val (newKey, left, right) = children[index].splitSelf(splitPoint)
        children[splitPoint] = right
        children.add(splitPoint, left)
        keys.add(splitPoint, newKey)
    }

    override fun splitSelf(splitPoint: Int): Triple<Int, BPlusTreeNode, BPlusTreeNode> {
        /* EXAMPLE:
         * keys:      2 4 6 8
         * children: 1 3 5 7 9
         * splitPoint: 2 (index in children)
         *
         * AFTER SPLIT:
         * PROMOTED KEY: 4
         * LEFT:
         * keys:      2
         * children: 1 3
         * RIGHT:
         * keys:      6 8
         * children: 5 7 9
         */
        val childrenOfLeft = LinkedList<BPlusTreeNode>()
        for (i in 0 until splitPoint) {
            childrenOfLeft.offerLast(children.pollFirst())
        }
        val keysOfLeft = LinkedList<Int>()
        for (i in 0 until splitPoint - 1) {
            keysOfLeft.offerLast(keys.pollFirst())
        }

        // NOTICE: this key is promoted to its parent since its left has no child.
        val promoted = keys.pollFirst()
        val left = BPlusInternalTreeNode(keysOfLeft, childrenOfLeft)
        return Triple(promoted, left, this)
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
}

class BPlusLeafNode(
        val records: PriorityQueue<Pair<Int, String>> = PriorityQueue<Pair<Int, String>>()
) : BPlusTreeNode {
    companion object {
        var cnt = 0
    }

    override val id: String
    var nextLeaf: BPlusLeafNode? = null

    init {
        id = "L_${cnt}"
        cnt++
    }

    override fun search(key: Int): Pair<Int, String>? {
        return records.find { it.first == key }
    }

    override fun insert(record: Pair<Int, String>): Boolean {
        if (search(record.first) != null) {
            return false // record already exists, insertion fails
        }
        records.offer(record)
        return true
    }

    override fun isFull(): Boolean {
        return records.size == order // non-standard, should be records.size-1==order
    }

    override fun splitSelf(splitPoint: Int): Triple<Int, BPlusTreeNode, BPlusTreeNode> {
        val recordsOfLeft = PriorityQueue<Pair<Int, String>>()
        for (i in 0..splitPoint) {
            recordsOfLeft.offer(records.poll())
        }

        val left = BPlusLeafNode(recordsOfLeft)
        left.nextLeaf = nextLeaf
        nextLeaf = left

        val promotedKey = records.first().first
        return Triple(promotedKey, left, this)
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