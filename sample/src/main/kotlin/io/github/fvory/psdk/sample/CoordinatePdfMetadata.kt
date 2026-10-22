package io.github.fvory.psdk.sample

import io.github.fvory.psdk.model.PageSize
import io.legere.pdfiumandroid.PdfiumCore
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object CoordinatePdfMetadata {
    // Read the actual source PDF; never substitute preview bitmap dimensions.
    fun read(bytes: ByteArray): List<PageSize> =
        PdfiumCore().newDocument(bytes).use { document ->
            List(document.getPageCount()) { index ->
                requireNotNull(document.openPage(index)).use { page ->
                    val media = page.getPageMediaBox()
                    val crop = page.getPageCropBox()
                    val box = if (abs(crop.width()) > 0 && abs(crop.height()) > 0) crop else media
                    val left = max(min(media.left, media.right), min(box.left, box.right))
                    val right = min(max(media.left, media.right), max(box.left, box.right))
                    val bottom = max(min(media.top, media.bottom), min(box.top, box.bottom))
                    val top = min(max(media.top, media.bottom), max(box.top, box.bottom))
                    displaySize(right - left, top - bottom, page.getPageRotation())
                }
            }
        }

    internal fun displaySize(
        width: Float,
        height: Float,
        quarterTurns: Int,
    ): PageSize {
        require(width.isFinite() && height.isFinite() && width > 0 && height > 0) { "Ukuran halaman tidak valid" }
        require(quarterTurns in 0..3) { "Rotasi halaman tidak valid" }
        val w = width.toString().toDouble()
        val h = height.toString().toDouble()
        return if (quarterTurns % 2 == 0) PageSize(w, h) else PageSize(h, w)
    }
}
