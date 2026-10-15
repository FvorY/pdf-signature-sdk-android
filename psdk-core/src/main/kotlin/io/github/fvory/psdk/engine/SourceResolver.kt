package io.github.fvory.psdk.engine

import android.content.Context
import io.github.fvory.psdk.error.PsdkException
import io.github.fvory.psdk.model.PdfSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Turns any [PdfSource] into a local, seekable file that `PdfRenderer` can open (spec §15.4).
 * Everything except [PdfSource.FileSource] is copied into the viewer's temp folder.
 */
internal class SourceResolver(
    private val context: Context,
    private val temp: TempFileManager,
) {
    suspend fun resolve(
        source: PdfSource,
        allowInsecureHttp: Boolean = false,
    ): File =
        withContext(Dispatchers.IO) {
            val file =
                when (source) {
                    is PdfSource.FileSource -> {
                        if (!source.file.isFile) throw PsdkException.InvalidSource("File not found: ${source.file}")
                        source.file
                    }
                    is PdfSource.UriSource -> {
                        val stream =
                            try {
                                context.contentResolver.openInputStream(source.uri)
                            } catch (e: Exception) {
                                throw PsdkException.InvalidSource("Cannot open ${source.uri}", e)
                            } ?: throw PsdkException.InvalidSource("Cannot open ${source.uri}")
                        copy(stream)
                    }
                    is PdfSource.AssetSource -> {
                        val stream =
                            try {
                                context.assets.open(source.path)
                            } catch (e: IOException) {
                                throw PsdkException.InvalidSource("Asset not found: ${source.path}", e)
                            }
                        copy(stream)
                    }
                    is PdfSource.BytesSource -> {
                        if (source.bytes.size.toLong() > MAX_DOCUMENT_BYTES) {
                            throw PsdkException.InvalidSource("The document exceeds the maximum size of 100 MiB.")
                        }
                        temp.newFile().apply { writeBytes(source.bytes) }
                    }
                    is PdfSource.UrlSource -> download(source, allowInsecureHttp)
                }
            PdfHeader.requirePdf(file)
            file
        }

    private fun copy(stream: InputStream): File {
        val target = temp.newFile()
        try {
            stream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_DOCUMENT_BYTES) {
                            throw PsdkException.InvalidSource("The document exceeds the maximum size of 100 MiB.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            return target
        } catch (e: Exception) {
            temp.delete(target)
            throw e
        }
    }

    private fun download(
        source: PdfSource.UrlSource,
        allowInsecureHttp: Boolean,
    ): File {
        val original = parseAndValidateUrl(source.url, allowInsecureHttp)
        var url = original
        for (redirect in 0..MAX_REDIRECTS) {
            val connection =
                try {
                    url.openConnection() as HttpURLConnection
                } catch (e: SecurityException) {
                    throw PsdkException.Network(null, "The app needs the INTERNET permission to load URLs.", e)
                }
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.instanceFollowRedirects = false
                // Headers (often auth tokens) are only sent to the original origin, also after redirects
                if (sameOrigin(url, original)) source.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                val code = connection.responseCode
                if (code in REDIRECT_CODES) {
                    if (redirect == MAX_REDIRECTS) {
                        throw PsdkException.Network(code, "Download failed: too many redirects from ${safeUrl(url)}")
                    }
                    val location =
                        connection.getHeaderField("Location")
                            ?: throw PsdkException.Network(code, "Download failed: redirect has no location")
                    url =
                        try {
                            parseAndValidateUrl(URL(url, location).toString(), allowInsecureHttp)
                        } catch (e: PsdkException) {
                            throw e
                        } catch (e: Exception) {
                            throw PsdkException.InvalidSource("Invalid redirect URL.", e)
                        }
                    continue
                }
                if (code !in 200..299) {
                    throw PsdkException.Network(code, "Download failed with HTTP $code: ${safeUrl(url)}")
                }
                if (connection.contentLengthLong > MAX_DOCUMENT_BYTES) {
                    throw PsdkException.InvalidSource("The document exceeds the maximum size of 100 MiB.")
                }
                return copy(connection.inputStream)
            } catch (e: PsdkException) {
                throw e
            } catch (e: SecurityException) {
                throw PsdkException.Network(null, "The app needs the INTERNET permission to load URLs.", e)
            } catch (e: IOException) {
                throw PsdkException.Network(null, "Download failed: ${safeUrl(url)}", e)
            } finally {
                connection.disconnect()
            }
        }
        error("unreachable")
    }

    private fun parseAndValidateUrl(
        value: String,
        allowInsecureHttp: Boolean,
    ): URL {
        val url =
            try {
                URL(value)
            } catch (e: MalformedURLException) {
                throw PsdkException.InvalidSource("Invalid URL.", e)
            }
        val protocol = url.protocol.lowercase()
        val allowed = protocol == "https" || (allowInsecureHttp && protocol == "http")
        if (!allowed || url.host.isNullOrBlank()) {
            throw PsdkException.InvalidSource("Only HTTPS URLs are supported: ${safeUrl(url)}")
        }
        if (!url.userInfo.isNullOrBlank()) {
            throw PsdkException.InvalidSource("URLs with embedded credentials are not supported: ${safeUrl(url)}")
        }
        return url
    }

    private fun sameOrigin(
        a: URL,
        b: URL,
    ): Boolean =
        a.protocol.equals(b.protocol, ignoreCase = true) &&
            a.host.equals(b.host, ignoreCase = true) &&
            (if (a.port == -1) a.defaultPort else a.port) == (if (b.port == -1) b.defaultPort else b.port)

    private fun safeUrl(url: URL): String {
        val port = if (url.port >= 0) ":${url.port}" else ""
        return "${url.protocol}://${url.host}$port${url.path.ifEmpty { "/" }}"
    }

    private companion object {
        const val TIMEOUT_MS = 30_000
        const val MAX_REDIRECTS = 5
        const val MAX_DOCUMENT_BYTES = 100L * 1024L * 1024L
        val REDIRECT_CODES = 300..399
    }
}
