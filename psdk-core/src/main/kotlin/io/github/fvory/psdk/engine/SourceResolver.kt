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
import java.net.URL

/**
 * Turns any [PdfSource] into a local, seekable file that `PdfRenderer` can open (spec §15.4).
 * Everything except [PdfSource.FileSource] is copied into the viewer's temp folder.
 */
internal class SourceResolver(
    private val context: Context,
    private val temp: TempFileManager,
) {
    suspend fun resolve(source: PdfSource): File =
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
                    is PdfSource.BytesSource -> temp.newFile().apply { writeBytes(source.bytes) }
                    is PdfSource.UrlSource -> download(source)
                }
            PdfHeader.requirePdf(file)
            file
        }

    private fun copy(stream: InputStream): File {
        val target = temp.newFile()
        stream.use { input -> target.outputStream().use { input.copyTo(it) } }
        return target
    }

    private fun download(source: PdfSource.UrlSource): File {
        val url =
            try {
                URL(source.url)
            } catch (e: Exception) {
                throw PsdkException.InvalidSource("Invalid URL: ${source.url}", e)
            }
        if (url.protocol != "http" && url.protocol != "https") {
            throw PsdkException.InvalidSource("Only http and https URLs are supported: ${source.url}")
        }
        val connection =
            try {
                url.openConnection() as HttpURLConnection
            } catch (e: SecurityException) {
                throw PsdkException.Network(null, "The app needs the INTERNET permission to load URLs.", e)
            }
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            source.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val code = connection.responseCode
            if (code >= 400) throw PsdkException.Network(code, "Download failed with HTTP $code: ${source.url}")
            return copy(connection.inputStream)
        } catch (e: PsdkException) {
            throw e
        } catch (e: SecurityException) {
            throw PsdkException.Network(null, "The app needs the INTERNET permission to load URLs.", e)
        } catch (e: IOException) {
            throw PsdkException.Network(null, "Download failed: ${source.url}", e)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000
    }
}
