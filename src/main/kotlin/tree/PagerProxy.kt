package tree

class PagerProxy(private val pager: Pager) {
    private val bitmap: ByteArray(pager.pageNum)
}