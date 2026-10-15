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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
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
    suspend fun load(
        image: PsdkImage,
        allowInsecureHttp: Boolean = false,
    ): Bitmap? {
        if (image is PsdkImage.BitmapImage) return image.bitmap
        val key = key(image)
        cache.get(key)?.let { return it }
        val bitmap =
            withContext(Dispatchers.IO) {
                try {
                    when (image) {
                        is PsdkImage.Url -> decode(download(image.url, allowInsecureHttp))
                        is PsdkImage.Base64 -> {
                            val payload = Base64Data.payload(image.data)
                            if (payload.length > MAX_BASE64_CHARS) throw IOException("Image is too large")
                            decode(Base64.decode(payload, Base64.DEFAULT))
                        }
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

    private fun download(
        value: String,
        allowInsecureHttp: Boolean,
    ): ByteArray {
        var url = parseAndValidateUrl(value, allowInsecureHttp)
        for (redirect in 0..MAX_REDIRECTS) {
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = false
                val code = connection.responseCode
                if (code in REDIRECT_CODES) {
                    if (redirect == MAX_REDIRECTS) throw IOException("Too many redirects")
                    val location =
                        connection.getHeaderField("Location") ?: throw IOException("Redirect has no location")
                    url = parseAndValidateUrl(URL(url, location).toString(), allowInsecureHttp)
                    continue
                }
                if (code !in 200..299) throw IOException("HTTP $code")
                if (connection.contentLengthLong > MAX_IMAGE_BYTES) throw IOException("Image is too large")
                return connection.inputStream.use { readLimited(it, MAX_IMAGE_BYTES) }
            } finally {
                connection.disconnect()
            }
        }
        error("unreachable")
    }

    private fun readLimited(
        input: java.io.InputStream,
        maxBytes: Long,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) throw IOException("Image is too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun parseAndValidateUrl(
        value: String,
        allowInsecureHttp: Boolean,
    ): URL {
        val url =
            try {
                URL(value)
            } catch (e: MalformedURLException) {
                throw IOException("Invalid image URL", e)
            }
        val protocol = url.protocol.lowercase()
        val allowed = protocol == "https" || (allowInsecureHttp && protocol == "http")
        if (!allowed || url.host.isNullOrBlank() || !url.userInfo.isNullOrBlank()) {
            throw IOException("Only HTTPS image URLs are supported")
        }
        return url
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private companion object {
        const val MAX_SIDE = 1024
        const val MAX_REDIRECTS = 5
        const val MAX_IMAGE_BYTES = 10L * 1024L * 1024L
        const val MAX_BASE64_CHARS = 14_000_000
        val REDIRECT_CODES = 300..399
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
