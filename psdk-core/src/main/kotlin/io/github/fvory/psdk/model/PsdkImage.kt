package io.github.fvory.psdk.model

import android.graphics.Bitmap
import androidx.annotation.DrawableRes

/** Image of a signature, E-Materai or picker option. */
public sealed interface PsdkImage {
    /** An `http(s)://` image. */
    public data class Url(
        public val url: String,
    ) : PsdkImage

    /** A `data:image/...;base64,...` URI or plain Base64 text. */
    public data class Base64(
        public val data: String,
    ) : PsdkImage

    public class BitmapImage(
        public val bitmap: Bitmap,
    ) : PsdkImage

    public data class Resource(
        @param:DrawableRes public val resId: Int,
    ) : PsdkImage

    public companion object {
        /**
         * Reads a string the way the web SDK's `image` option is used: `http(s)://` is a URL,
         * anything else (`data:` URI or plain Base64) is Base64 data.
         */
        public fun of(value: String): PsdkImage {
            val trimmed = value.trim()
            return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                Url(trimmed)
            } else {
                Base64(trimmed)
            }
        }
    }
}
