package recordManager

import pager.Pager

class Tree(pager: Pager) : RecordManager {
    private val pagerProxy = PagerProxy(pager)
    private var root: TreeNode = LeafNode(pagerProxy)

    init {
        pagerProxy.persistTreeNode(root)
    }

    override fun search(key: Int): DatabaseRecord? {
        return root.search(key)
    }

    override fun insert(record: DatabaseRecord): Boolean {
        if (root.isFull()) { // this is when the tree is heightened
            val (key, left, right) = root.splitSelf(root.order / 2)
            root = InternalTreeNode(pagerProxy, key, left.id, right.id)
        }
        return root.insert(record)
    }

    override fun delete(key: Int): Boolean {
        return root.delete(key)
    }

    override fun set(record: DatabaseRecord): Boolean {
        return true
    }

    fun snapshot() = root.snapshot()
}