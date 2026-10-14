package io.github.fvory.psdk.cache

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Rendered pages, limited by memory: at most 1/8 of the app heap and 128 MB (spec §15.1 R6).
 * Evicted bitmaps are not recycled because Compose may still be drawing them.
 */
internal class BitmapCache(
    maxBytes: Int = defaultMaxBytes(),
) {
    private val cache =
        object : LruCache<String, Bitmap>(maxBytes) {
            override fun sizeOf(
                key: String,
                value: Bitmap,
            ): Int = value.allocationByteCount
        }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(
        key: String,
        bitmap: Bitmap,
    ) {
        cache.put(key, bitmap)
    }

    fun clear() {
        cache.evictAll()
    }

    private companion object {
        fun defaultMaxBytes(): Int {
            val eighthOfHeap = Runtime.getRuntime().maxMemory() / 8
            return minOf(eighthOfHeap, 128L * 1024 * 1024).toInt()
        }
    }
}
