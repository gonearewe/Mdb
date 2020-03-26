package recordManager

import java.util.*

interface TreeNode {
    val id: Int // unique id for identifying tree node

    // an internal node has k children and k-1 keys, order/2 <= k <= order
    val order get() = 9

    // destruct will be called when this node is no longer needed.
    fun destruct()

    // core api for B+ Tree

    fun search(recordKey: Int): DatabaseRecord?
    fun insert(record: DatabaseRecord): Boolean
    fun set(record: DatabaseRecord): Boolean

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
    fun splitSelf(splitPoint: Int): Triple<Int, TreeNode, TreeNode>

    // helper methods for delete

    // isHalfEmpty tells if the node only has the least number of children
    // required, in which case, one more child deleted will lead to the loss of balance.
    fun isHalfEmpty(): Boolean

    // lends the first or the last child(tree node) along with one key to balance its brother node,
    // a demoted key from its parent is also provided to supply the loss,
    // and returns one key to the parent for adjustment.
    fun lendItsFirstTo(demotedKey: Int, brother: TreeNode): Int
    fun lendItsLastTo(demotedKey: Int, brother: TreeNode): Int

    // mergeWith merges itself with other node and returns the product,
    // the provided key demoted from its parent will be inserted to keep the structure.
    fun mergeWith(demotedKey: Int, other: TreeNode): TreeNode
}

class InternalTreeNode internal constructor(
        private val pager: PagerProxy,
        private val keys: LinkedList<Int> = LinkedList<Int>(),
        private val children: LinkedList<Int> = LinkedList<Int>()
) : TreeNode {
    override val id: Int = pager.distributeID()

    constructor(pager: PagerProxy, oneKey: Int, leftChildID: Int, rightChildID: Int) : this(pager) {
        keys.add(oneKey)
        children.add(leftChildID)
        children.add(rightChildID)
    }

    override fun destruct() {
        pager.retrieve(id) // give back node ID distributed when initialised
    }

    override fun search(recordKey: Int) = loadChild(children[locateChildIndex(recordKey)]).search(recordKey)

    override fun set(record: DatabaseRecord) = loadChild(children[locateChildIndex(record.key)]).set(record)

    override fun insert(record: DatabaseRecord): Boolean {
        val index = locateChildIndex(record.key)
        val child = loadChild(children[index]) // NOTE: I/O read
        if (!child.isFull()) { // it's a simple case
            return child.insert(record)
        }

        splitChildAt(index, child)
        // now children[index] is a new child (left part of the old one)
        // and so is children[index+1] (right part of the old one)

        // in fact, we only need to check the two new keys, but whatever
        val newIndex = locateChildIndex(record.key)
        // NOTE: I/O read, can be optimised actually, see comments in splitSelf
        return loadChild(children[newIndex]).insert(record) // now it's apparently not full, just insert
    }

    override fun delete(key: Int): Boolean {
        val index = locateChildIndex(key)
        val child = loadChild(children[index])
        if (!child.isHalfEmpty()) {
            return child.delete(key) // the simple case
        }

        // The target child is half-empty, it's possible that it loses balance after deletion,
        // in which case we may need to backtrace. Well, to avoid that, we handle it with the help
        // of one of its brother node to make sure that it has one more children than 'half-empty'.
        // After this beforehand process, we don't need any backtrack.
        if (index != children.size - 1) { // this child has a brother to its right
            val brother = loadChild(children[index + 1])
            if (brother.isHalfEmpty()) { // we will merge it with its 'right'(literally) brother
                mergeChildrenAt(index, child, index + 1, brother)
            } else { // we can borrow from the 'right'(literally) brother
                childrenBorrow(to = index, toChild = child, from = index + 1, fromChild = brother)
            }
        } else { // unfortunately, this child is the right most one, we can only turn to its left brother
            // don't worry, for B+ Tree, if we don't have a right brother, we must have a left one,
            // since it's impossible that there is only node on any depth(well, except the root node)
            val brother = loadChild(children[index - 1])
            if (brother.isHalfEmpty()) { // we will merge it with its left brother
                mergeChildrenAt(index, child, index - 1, brother)
            } else { // we can borrow from the left brother
                childrenBorrow(to = index, toChild = child, from = index - 1, fromChild = brother)
            }
        }

        // NOTE: end of the modification of this, I/O write
        pager.persistTreeNode(this)

        val newIndex = locateChildIndex(key)
        // NOTE: one I/O read
        return loadChild(children[newIndex]).delete(key)
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

    private fun loadChild(id: Int) = pager.loadTreeNode(id)

    // splitChildAt splits child at given index into two nodes and adds a new key to self,
    // child is provided to avoid redundant I/O read.
    private fun splitChildAt(index: Int, child: TreeNode) {
        val splitPoint = order / 2  // it belongs to the new right child
        val (newKey, left, right) = child.splitSelf(splitPoint)
        children[index] = right.id
        children.add(index, left.id)
        keys.add(index, newKey)
    }

    override fun splitSelf(splitPoint: Int): Triple<Int, TreeNode, TreeNode> {
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
        val childrenOfLeft = LinkedList<Int>()
        for (i in 0 until splitPoint) {
            childrenOfLeft.offerLast(children.pollFirst())
        }
        val keysOfLeft = LinkedList<Int>()
        for (i in 0 until splitPoint - 1) {
            keysOfLeft.offerLast(keys.pollFirst())
        }

        // NOTICE: this key is promoted to its parent since its left has no child.
        val promoted = keys.pollFirst()
        val left = InternalTreeNode(pager, keysOfLeft, childrenOfLeft)
        // NOTICE: two I/O write, actually you can optimise it into one write
        // if you choose to decide which child to process next right now
        pager.persistTreeNode(left)
        pager.persistTreeNode(this)

        return Triple(promoted, left, this)
    }

    override fun isHalfEmpty(): Boolean {
        return children.size <= order / 2 // should be equation in fact
    }

    // childrenBorrow let children[to] borrow a node and a key from children[from],
    // children[to] is a brother of children[from] which means abs(from-to) == 1.
    private fun childrenBorrow(to: Int, toChild: TreeNode, from: Int, fromChild: TreeNode) {
        if (from > to) { // borrow from right brother to child on the left
            // let the two children exchange
            val promotedkey = fromChild.lendItsFirstTo(demotedKey = keys[to], brother = toChild)
            keys[to] = promotedkey // update key
        } else { // borrow from left brother to child on the right
            // let the two children exchange
            val promotedkey = fromChild.lendItsLastTo(demotedKey = keys[from], brother = toChild)
            keys[from] = promotedkey // update key
        }
    }

    override fun lendItsFirstTo(demotedKey: Int, brother: TreeNode): Int {
        val to = brother as InternalTreeNode // asserting is good to avoid potential mistakes
        to.children.addLast(children.pollFirst()) // lend my first tree node to my brother
        to.keys.addLast(demotedKey) // my brother also needs an extra key to keep its structure
        val promotedKey = keys.pollFirst() // now my first key doesn't have a left child, promote it
        // NOTE: persist me and my brother, two I/O write
        pager.persistTreeNode(this)
        pager.persistTreeNode(to)

        return promotedKey
    }

    override fun lendItsLastTo(demotedKey: Int, brother: TreeNode): Int {
        val to = brother as InternalTreeNode // asserting is good to avoid potential mistakes
        to.children.addFirst(children.pollLast()) // lend my last tree node to my brother
        to.keys.addFirst(demotedKey) // my brother also needs an extra key to keep its structure
        val promotedKey = keys.pollLast()
        // NOTE: persist me and my brother, two I/O write
        pager.persistTreeNode(this)
        pager.persistTreeNode(to)

        return promotedKey // now my last key doesn't have a right child, promote it
    }

    // mergeChildren merges two half-empty nearby(meaning abs(a-b) == 1) children
    // at index a and b into one child,
    // one key will be demoted to them for insertion to keep the child's structure,
    // aNode and bNode are provided to avoid redundant I/O read.
    private fun mergeChildrenAt(a: Int, aNode: TreeNode, b: Int, bNode: TreeNode) {
        val l = if (a > b) b else a // index of node on the left
        val lNode = if (a > b) bNode else aNode // node on the left
        children[l] = aNode.mergeWith(demotedKey = keys[l], other = bNode).id // the merged one child
        children.removeAt(l + 1)
        keys.removeAt(l) // remove the demoted key
    }

    override fun mergeWith(demotedKey: Int, other: TreeNode): TreeNode {
        val node = other as InternalTreeNode // for B+ Tree, nodes on the same depth are isomorphic
        keys.addLast(demotedKey) // insert in the middle
        keys.addAll(node.keys)
        children.addAll(node.children)
        // NOTE: I/O write
        pager.persistTreeNode(this)
        // NOTE: destroy useless node
        node.destruct()
        return this
    }

    override fun snapshot(): MutableList<String> {
        return if (children.isEmpty()) ArrayList<String>() else loadChild(children[0]).snapshot()
    }

    override fun toString(): String {
        return buildString {
            append("id=$id,")
            append("keys= $keys, ")
            append("children= ")
            children.forEach {
                append(loadChild(it).id)
                append(", ")
            }

            append("\n")
            children.forEach {
                append(loadChild(it).toString())
            }
        }
    }
}

class LeafNode(
        private val pager: PagerProxy,
        private var records: DatabaseRecords = DatabaseRecords()
) : TreeNode {
    override val id: Int = pager.distributeID()
    private var nextLeaf: Int = -1024 // I don't have a nextLeaf on initialization
    private val hasNextLeaf
        get() = nextLeaf >= 0 && nextLeaf != id

    override fun destruct() {
        pager.retrieve(id) // give back node ID distributed when initialised
    }

    override fun search(recordKey: Int): DatabaseRecord? {
        return records.find { it.key == recordKey }
    }

    override fun set(record: DatabaseRecord): Boolean {
        val r = records.find { it.key == record.key }
        return if (r != null) {
            r.value = record.value
            true
        } else {
            false
        }
    }

    override fun insert(record: DatabaseRecord): Boolean {
        if (search(record.key) != null) {
            return false // record already exists, insertion fails
        }
        records.offer(record)
        // NOTE: I/O write
        pager.persistTreeNode(this)

        return true
    }

    override fun delete(key: Int): Boolean {
        if (records.removeIf { it.key == key }) {
            // NOTE: I/O write
            pager.persistTreeNode(this)
            return true
        } else {
            return false // not found, nothing changed for this node
        }
    }

    override fun isFull(): Boolean {
        return records.size == order // non-standard, should be records.size-1==order
    }

    override fun splitSelf(splitPoint: Int): Triple<Int, TreeNode, TreeNode> {
        val recordsOfLeft = DatabaseRecords()
        for (i in 0..splitPoint) {
            recordsOfLeft.offer(records.poll())
        }

        // exchange records in the node
        val rightLeaf = LeafNode(pager, this.records) // generate a right leaf
        this.records = recordsOfLeft // this becomes left leaf
        // insert right leaf into the 'nextLeaf' linked list
        rightLeaf.nextLeaf = nextLeaf
        nextLeaf = rightLeaf.id

        // NOTE: two I/O write
        pager.persistTreeNode(this)
        pager.persistTreeNode(rightLeaf)

        val promotedKey = rightLeaf.records.first().key
        return Triple(promotedKey, this, rightLeaf)
    }

    override fun isHalfEmpty(): Boolean {
        return records.size <= order / 2 // should be equation in fact
    }

    override fun lendItsFirstTo(demotedKey: Int, brother: TreeNode): Int {
        // leaf node doesn't need a demoted key
        val to = brother as LeafNode // asserting is good to avoid potential mistakes
        to.records.offer(records.poll())

        // NOTE: two I/O write
        pager.persistTreeNode(this)
        pager.persistTreeNode(to)

        return records.peek().key
    }

    override fun lendItsLastTo(demotedKey: Int, brother: TreeNode): Int {
        // leaf node doesn't need a demoted key
        val to = brother as LeafNode // asserting is good to avoid potential mistakes

        val remainings = DatabaseRecords()
        for (i in 0 until records.size - 1) {
            remainings.offer(records.poll())
        }

        to.records.offer(records.poll()) // lend my brother my last tree node
        records = remainings // remove the last element
        // NOTE: two I/O write
        pager.persistTreeNode(this)
        pager.persistTreeNode(to)

        return to.records.peek().key
    }

    override fun mergeWith(demotedKey: Int, other: TreeNode): TreeNode {
        // for a leaf node, we don't need the demoted key
        val otherNode = other as LeafNode
        val left = if (this.nextLeaf == otherNode.id) this else otherNode
        val right = if (this.nextLeaf == otherNode.id) otherNode else this
        assert(left.nextLeaf == right.id)

        left.records.addAll((right.records))// for B+ Tree, nodes on the same depth are isomorphic
        left.nextLeaf = right.nextLeaf

        right.nextLeaf = -1024 // right node is no longer needed
        right.destruct()

        return left
    }

    override fun snapshot(): MutableList<String> {
        return ArrayList<String>().also {
            it.add(records.toString())
            if (hasNextLeaf) {
                it.addAll(loadNextLeaf().snapshot())
            }
        }
    }

    private fun loadNextLeaf(): TreeNode {
        // NOTE: I/O read
        return pager.loadTreeNode(nextLeaf)
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

class DatabaseRecord(key: Int, value: String) {
    private val record = Pair<Int, String>(key, value)
    val key = record.first
    var value = record.second
}

class DatabaseRecords() : PriorityQueue<DatabaseRecord>(
        kotlin.Comparator<DatabaseRecord> { a: DatabaseRecord, b: DatabaseRecord ->
            a.key - b.key // so that the top's key is the minimum
        }
)