package pager

import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

private val PAGE_SIZE = 1024 // measured by byte
private val MAX_PAGE_NUM = 12 // how many pages to create during initialization

class MmapPager private constructor(
        private val file: RandomAccessFile,
        private val byteBuffer: MappedByteBuffer
) : Pager {
    companion object {
        fun of(filepath: String): MmapPager? {
            try {
                val file = RandomAccessFile(filepath, "rw")
                val byteBuffer = file.channel.map(FileChannel.MapMode.READ_WRITE, 0, (MAX_PAGE_NUM * PAGE_SIZE).toLong())
                return MmapPager(file, byteBuffer)
            } catch (e: IOException) {
                return null
            }
        }
    }

    override fun read(pageID: Int): ByteArray {
        val data = ByteArray(PAGE_SIZE)
        byteBuffer.get(data, pageID * PAGE_SIZE, PAGE_SIZE)
        return data
    }

    override fun write(pageID: Int, page: ByteArray): Boolean {
        byteBuffer.put(page, pageID * PAGE_SIZE, PAGE_SIZE)
        return true
    }
}