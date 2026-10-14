package io.github.fvory.psdk.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import io.github.fvory.psdk.model.PsdkImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads item and picker images (spec §15.1 R9). Large images are downsampled to 1024 px and the
 * results are cached by source, so the same signature image is decoded once.
 */
internal class ImageLoader(
    private val context: Context,
) {
    private val cache = LruCache<String, Bitmap>(16)

    /** Returns null when the image cannot be loaded; the item then keeps its placeholder look. */
    suspend fun load(image: PsdkImage): Bitmap? {
        if (image is PsdkImage.BitmapImage) return image.bitmap
        val key = key(image)
        cache.get(key)?.let { return it }
        val bitmap =
            withContext(Dispatchers.IO) {
                try {
                    when (image) {
                        is PsdkImage.Url -> decode(download(image.url))
                        is PsdkImage.Base64 -> decode(Base64.decode(Base64Data.payload(image.data), Base64.DEFAULT))
                        is PsdkImage.Resource -> ContextCompat.getDrawable(context, image.resId)?.toBitmap()
                        is PsdkImage.BitmapImage -> image.bitmap
                    }
                } catch (e: Exception) {
                    null
                }
            }
        bitmap?.let { cache.put(key, it) }
        return bitmap
    }

    private fun key(image: PsdkImage): String =
        when (image) {
            is PsdkImage.Url -> "url:${image.url}"
            is PsdkImage.Base64 -> "b64:${image.data.length}:${image.data.hashCode()}"
            is PsdkImage.Resource -> "res:${image.resId}"
            is PsdkImage.BitmapImage -> "bmp:${System.identityHashCode(image.bitmap)}"
        }

    private fun download(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private companion object {
        const val MAX_SIDE = 1024
    }
}

/** Splits `data:image/png;base64,AAAA` into its Base64 part. Plain Base64 is returned as is. */
internal object Base64Data {
    fun payload(data: String): String {
        val trimmed = data.trim()
        if (!trimmed.startsWith("data:", ignoreCase = true)) return trimmed
        val comma = trimmed.indexOf(',')
        return if (comma >= 0) trimmed.substring(comma + 1) else ""
    }
}
