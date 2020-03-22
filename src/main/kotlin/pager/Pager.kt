package pager

interface Pager {
    fun read(pageID: Int): ByteArray
    fun write(pageID: Int, page: ByteArray): Boolean
}