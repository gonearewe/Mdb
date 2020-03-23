package pager

interface Pager {
    val pageNum: Int
    val pageSize: Int
    fun read(pageID: Int): ByteArray
    fun write(pageID: Int, page: ByteArray): Boolean
}