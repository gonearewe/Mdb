package tree

import java.util.*
import kotlin.collections.ArrayList

class Algorithm private constructor() {
    lateinit var root: BPlusTreeNode

    companion object {
        fun newBPlusTree(): Algorithm {
            val tree = Algorithm()
            tree.root = BPlusLeafNode()
            return tree
        }
    }

    fun search(key: Int): Pair<Int, String>? {
        return root.search(key)
    }

    fun insert(record: Pair<Int, String>): Boolean {
        if (root.isFull()) { // this is when the tree is heightened
            val (key, left, right) = root.splitSelf(root.order / 2)
            root = BPlusInternalTreeNode(key, left, right)
        }
        return root.insert(record)
    }

    fun delete(key: Int): Boolean {
        return root.delete(key)
    }

    fun snapshot() = root.snapshot()
}

interface BPlusTreeNode {
    val id: String

    // an internal node has k children and k-1 keys, order/2 <= k <= order
    val order get() = 9

    // core api for B+ Tree

    fun search(key: Int): Pair<Int, String>?
    fun insert(record: Pair<Int, String>): Boolean

    // delete deletes a record according to the given key and the
    // returned value tells if any record is actually deleted(meaning the record may not exists).
    fun delete(key: Int): Boolean

    // methods for DEBUG
    fun snapshot(): MutableList<String>

    // helper methods for insert

    fun isFull(): Boolean

    // splitSelf splits current node to two nodes and returns them along
    // with one key which is greater than all keys in the left child and
    // no greater than right ones.
    // first: promoted key, second: left node, third: right node
    fun splitSelf(splitPoint: Int): Triple<Int, BPlusTreeNode, BPlusTreeNode>

    // helper methods for delete

    // isHalfEmpty tells if the node only has the least number of children
    // required, in which case, one more child deleted will lead to the loss of balance.
    fun isHalfEmpty(): Boolean

    // lends the first or the last child(tree node) along with one key to balance its brother node,
    // a demoted key from its parent is also provided to supply the loss,
    // and returns one key to the parent for adjustment.
    fun lendItsFirstTo(demotedKey: Int, brother: BPlusTreeNode): Int
    fun lendItsLastTo(demotedKey: Int, brother: BPlusTreeNode): Int

    // mergeWith merges itself with other node and returns the product,
    // the provided key demoted from its parent will be inserted to keep the structure.
    fun mergeWith(demotedKey: Int, other: BPlusTreeNode): BPlusTreeNode
}

class BPlusInternalTreeNode internal constructor(
        private val keys: LinkedList<Int> = LinkedList<Int>(),
        private val children: LinkedList<BPlusTreeNode> = LinkedList<BPlusTreeNode>()
) : BPlusTreeNode {
    companion object {
        var cnt = 0
    }

    override val id: String


    init {
        id = "I_$cnt"
        cnt++
    }

    constructor(oneKey: Int, leftChild: BPlusTreeNode, rightChild: BPlusTreeNode) : this() {
        keys.add(oneKey)
        children.add(leftChild)
        children.add(rightChild)
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

    override fun delete(key: Int): Boolean {
        val index = locateChildIndex(key)
        if (!children[index].isHalfEmpty()) {
            return children[index].delete(key) // the simple case
        }

        // The target child is half-empty, it's possible that it loses balance after deletion,
        // in which case we may need to backtrace. Well, to avoid that, we handle it with the help
        // of one of its brother node to make sure that it has one more children than 'half-empty'.
        // After this beforehand process, we don't need any backtrack.
        if (index != children.size - 1) { // this child has a brother to its right
            if (children[index + 1].isHalfEmpty()) { // we will merge it with its 'right'(literally) brother
                mergeChildrenAt(index, index + 1)
            } else { // we can borrow from the 'right'(literally) brother
                childrenBorrow(to = index, from = index + 1)
            }
        } else { // unfortunately, this child is the right most one, we can only turn to its left brother
            // don't worry, for B+ Tree, if we don't have a right brother, we must have a left one,
            // since it's impossible that there is only node on any depth(well, except the root node)
            if (children[index - 1].isHalfEmpty()) { // we will merge it with its left brother
                mergeChildrenAt(index, index - 1)
            } else { // we can borrow from the left brother
                childrenBorrow(to = index, from = index - 1)
            }
        }

        val newIndex = locateChildIndex(key)
        return children[newIndex].delete(key)
    }

    override fun isFull(): Boolean {
        return children.size == order
    }

    // locateChildIndex queries current node's keys to tell which child the key belongs to.
    private fun locateChildIndex(key: Int): Int {
        for (index in 0 until keys.size) {
            if (keys[index] > key) {
                return index
            }
        }

        return children.size - 1 // new key is greater than any keys, append to the last child
    }

    // splitChildAt splits child at given index into two nodes and adds a new key to self.
    private fun splitChildAt(index: Int) {
        val splitPoint = order / 2  // it belongs to the new right child
        val (newKey, left, right) = children[index].splitSelf(splitPoint)
        children[index] = right
        children.add(index, left)
        keys.add(index, newKey)
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

    override fun isHalfEmpty(): Boolean {
        return children.size <= order / 2 // should be equation in fact
    }

    // childrenBorrow let children[to] borrow a node and a key from children[from],
    // children[to] is a brother of children[from] which means abs(from-to) == 1.
    private fun childrenBorrow(to: Int, from: Int) {
        if (from > to) { // borrow from right brother to child on the left
            // let the two children exchange
            val promotedkey = children[from].lendItsFirstTo(demotedKey = keys[to], brother = children[to])
            keys[to] = promotedkey // update key
        } else { // borrow from left brother to child on the right
            // let the two children exchange
            val promotedkey = children[from].lendItsLastTo(demotedKey = keys[from], brother = children[to])
            keys[from] = promotedkey // update key
        }
    }

    override fun lendItsFirstTo(demotedKey: Int, brother: BPlusTreeNode): Int {
        val to = brother as BPlusInternalTreeNode // asserting is good to avoid potential mistakes
        to.children.addLast(children.pollFirst()) // lend my first tree node to my brother
        to.keys.addLast(demotedKey) // my brother also needs an extra key to keep its structure
        return keys.pollFirst() // now my first key doesn't have a left child, promote it
    }

    override fun lendItsLastTo(demotedKey: Int, brother: BPlusTreeNode): Int {
        val to = brother as BPlusInternalTreeNode // asserting is good to avoid potential mistakes
        to.children.addFirst(children.pollLast()) // lend my last tree node to my brother
        to.keys.addFirst(demotedKey) // my brother also needs an extra key to keep its structure
        return keys.pollLast() // now my last key doesn't have a right child, promote it
    }

    // mergeChildren merges two half-empty nearby(meaning abs(a-b) == 1) children
    // at index a and b into one child,
    // one key will be demoted to them for insertion to keep the child's structure.
    private fun mergeChildrenAt(a: Int, b: Int) {
        val l = if (a > b) b else a
        children[l] = children[a].mergeWith(demotedKey = keys[l], other = children[b]) // the merged one child
        children.removeAt(l + 1)
        keys.removeAt(l) // remove the demoted key
    }

    override fun mergeWith(demotedKey: Int, other: BPlusTreeNode): BPlusTreeNode {
        val node = other as BPlusInternalTreeNode // for B+ Tree, nodes on the same depth are isomorphic
        keys.addLast(demotedKey) // insert in the middle
        keys.addAll(node.keys)
        children.addAll(node.children)
        return this
    }

    override fun snapshot(): MutableList<String> {
        return if (children.isEmpty()) ArrayList<String>() else children[0].snapshot()
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
        var records: Records<String> = Records<String>()
) : BPlusTreeNode {
    companion object {
        var cnt = 0
    }

    override val id: String
    private var nextLeaf: BPlusLeafNode? = null

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

    override fun delete(key: Int): Boolean {
        return records.removeIf { it.first == key }
    }

    override fun isFull(): Boolean {
        return records.size == order // non-standard, should be records.size-1==order
    }

    override fun splitSelf(splitPoint: Int): Triple<Int, BPlusTreeNode, BPlusTreeNode> {
        val recordsOfLeft = Records<String>()
        for (i in 0..splitPoint) {
            recordsOfLeft.offer(records.poll())
        }

        // exchange records in the node
        val rightLeaf = BPlusLeafNode(this.records) // generate a right leaf
        this.records = recordsOfLeft // this becomes left leaf
        // insert right leaf into the 'nextLeaf' linked list
        rightLeaf.nextLeaf = nextLeaf
        nextLeaf = rightLeaf

        val promotedKey = rightLeaf.records.first().first
        return Triple(promotedKey, this, rightLeaf)
    }

    override fun isHalfEmpty(): Boolean {
        return records.size <= order / 2 // should be equation in fact
    }

    override fun lendItsFirstTo(demotedKey: Int, brother: BPlusTreeNode): Int {
        // leaf node doesn't need a demoted key
        val to = brother as BPlusLeafNode // asserting is good to avoid potential mistakes
        to.records.offer(records.poll())
        return records.peek().first
    }

    override fun lendItsLastTo(demotedKey: Int, brother: BPlusTreeNode): Int {
        // leaf node doesn't need a demoted key
        val to = brother as BPlusLeafNode // asserting is good to avoid potential mistakes

        val remainings = Records<String>()
        for (i in 0 until records.size - 1) {
            remainings.offer(records.poll())
        }

        to.records.offer(records.poll()) // lend my brother my last tree node
        records = remainings // remove the last element
        return to.records.peek().first
    }

    override fun mergeWith(demotedKey: Int, other: BPlusTreeNode): BPlusTreeNode {
        // for a leaf node, we don't need the demoted key
        val otherNode = other as BPlusLeafNode
        val left = if (this.nextLeaf == otherNode) this else otherNode
        val right = if (this.nextLeaf == otherNode) otherNode else this
        assert(left.nextLeaf == right)

        left.records.addAll((right.records))// for B+ Tree, nodes on the same depth are isomorphic
        left.nextLeaf = right.nextLeaf
        right.nextLeaf = null

        return left
    }

    override fun snapshot(): MutableList<String> {
        return ArrayList<String>().also {
            it.add(records.toString())
            if (nextLeaf != null) {
                it.addAll(nextLeaf!!.snapshot())
            }
        }
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

class Records<V>() : PriorityQueue<Pair<Int, V>>(
        kotlin.Comparator<Pair<Int, V>> { a: Pair<Int, V>, b: Pair<Int, V> ->
            a.first - b.first // so that the top's key is the minimum
        }
)

fun main() {
}