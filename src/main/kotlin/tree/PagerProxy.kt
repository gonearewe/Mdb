package tree

import pager.Pager
import java.io.*
import java.util.*

class PagerProxy(private val pager: Pager) {
    private val bitmap = BooleanArray(pager.pageNum)
    private val firstUnsetID
        get() = bitmap.indexOfFirst { !it }.also { bitmap[it] = true }

    fun distributeID(): Int {
        return firstUnsetID
    }

    fun retrieve(id: Int) {
        clear(id)
    }

    fun persistTreeNode(node: TreeNode) {
        val bytes = ByteArrayOutputStream(pager.pageSize)
        ObjectOutputStream(bytes).writeObject(node)
        if (bytes.size() > pager.pageSize) {
            throw TreeException("tree node too large to persist: ${bytes.size()} out of ${pager.pageSize}(limit)")
        }
        pager.write(node.id, Arrays.copyOf(bytes.toByteArray(), pager.pageSize))
    }

    fun loadTreeNode(id: Int): TreeNode {
        try {
            return ObjectInputStream(ByteArrayInputStream(pager.read(id))).readObject() as TreeNode
        } catch (e: IOException) {
            throw TreeException("load tree node (id: $id}): ${e.message}")
        }
    }

    private fun set(id: Int) {
        bitmap[id] = true
    }

    private fun clear(id: Int) {
        bitmap[id] = false
    }
}