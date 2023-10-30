package libpag.pagviewer

import org.libpag.PAGFile

public class PAGFileCache(private val maxFiles: Int) {
    private val mMap: LinkedHashMap<String, PAGFile?> = object : LinkedHashMap<String, PAGFile?>(maxFiles, 0.75F, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PAGFile?>?): Boolean {
            return size > maxFiles
        }
    }

    @Synchronized
    public fun getOrPut(path: String, creator: () -> PAGFile?): PAGFile? {
        return mMap.getOrPut(path, creator)
    }
}
