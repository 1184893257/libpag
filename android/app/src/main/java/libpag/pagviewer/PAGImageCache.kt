package libpag.pagviewer

import org.libpag.PAGImage

public class PAGImageCache(private val maxBytes: Long) {
    private var bytes: Long = 0

    private val mMap: LinkedHashMap<String, PAGImage?> = object : LinkedHashMap<String, PAGImage?>(20, 0.75F, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PAGImage?>?): Boolean {
            val it = entries.iterator()
            while (bytes > maxBytes && it.hasNext()) {
                bytes -= getBytes(it.next().value)
                it.remove()
            }
            return false
        }
    }

    @Synchronized
    public fun getOrPut(path: String, creator: () -> PAGImage?): PAGImage? {
        return mMap.getOrPut(path) {
            val image = creator()
            bytes += getBytes(image)
            image
        }
    }

    private fun getBytes(image: PAGImage?): Long {
        if (image == null) {
            return 0L
        }
        return image.width().toLong() * image.height() * 4
    }
}
