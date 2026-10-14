package io.github.fvory.psdk.engine

import io.github.fvory.psdk.error.PsdkException
import java.io.File

/** Checks for the `%PDF-` marker. Like most readers, it may appear anywhere in the first 1024 bytes. */
internal object PdfHeader {
    private val MARKER = "%PDF-".toByteArray(Charsets.US_ASCII)
    private const val SEARCH_LIMIT = 1024

    fun isPdf(file: File): Boolean {
        val head = ByteArray(SEARCH_LIMIT)
        val read = file.inputStream().use { it.read(head) }
        if (read < MARKER.size) return false
        return (0..read - MARKER.size).any { start ->
            MARKER.indices.all { head[start + it] == MARKER[it] }
        }
    }

    fun requirePdf(file: File) {
        if (!isPdf(file)) throw PsdkException.NotAPdf()
    }
}
