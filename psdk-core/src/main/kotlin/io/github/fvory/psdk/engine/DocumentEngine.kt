package io.github.fvory.psdk.engine

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import io.github.fvory.psdk.model.PageSize

/**
 * Renders an opened PDF. Implemented with `PdfRenderer` or the optional Pdfium module.
 * Public only so psdk-pdfium can implement it; not part of the SDK's supported API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface DocumentEngine {
    public val pageCount: Int

    /** Size in PDF points after `/Rotate`. [page] is 1-based. */
    public fun pageSize(page: Int): PageSize

    /** Renders [page] (1-based) scaled to fill a [width] × [height] px bitmap with a white background. */
    public suspend fun render(
        page: Int,
        width: Int,
        height: Int,
    ): Bitmap

    /** Releases the document. Safe to call while a render is running and more than once. */
    public fun close()
}
