package io.github.fvory.psdk.engine

import android.graphics.Bitmap
import io.github.fvory.psdk.model.PageSize

/** Renders an opened PDF. Implemented with `PdfRenderer` here and with Pdfium in `psdk-pdfium`. */
internal interface DocumentEngine {
    val pageCount: Int

    /** Size in PDF points after `/Rotate`. [page] is 1-based. */
    fun pageSize(page: Int): PageSize

    /** Renders [page] (1-based) scaled to fill a [width] × [height] px bitmap with a white background. */
    suspend fun render(
        page: Int,
        width: Int,
        height: Int,
    ): Bitmap

    /** Releases the document. Safe to call while a render is running and more than once. */
    fun close()
}
