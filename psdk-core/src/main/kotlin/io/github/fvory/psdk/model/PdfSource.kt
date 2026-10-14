package io.github.fvory.psdk.model

import android.net.Uri
import java.io.File

/** Where a PDF document comes from. Passed to `PdfSignatureViewer.loadDocument`. */
public sealed interface PdfSource {
    /** A file the app can read directly. */
    public data class FileSource(
        public val file: File,
    ) : PdfSource

    /** A `content://` or `file://` URI, e.g. from the system file picker. Copied to the cache before opening. */
    public data class UriSource(
        public val uri: Uri,
    ) : PdfSource

    /** An `http(s)://` URL. Downloaded to the cache first; the app needs the INTERNET permission. */
    public data class UrlSource(
        public val url: String,
        public val headers: Map<String, String> = emptyMap(),
    ) : PdfSource

    /** The document bytes, e.g. received from an API. */
    public class BytesSource(
        public val bytes: ByteArray,
    ) : PdfSource

    /** A file in the app's `assets/` folder. */
    public data class AssetSource(
        public val path: String,
    ) : PdfSource
}
