package io.github.fvory.psdk.engine

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A rectangle in pixels of a page drawn at full size, right/bottom exclusive. */
internal data class PixelRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Sharp rendering at high zoom (spec §15.1 R2). The whole-page bitmap is capped by [RenderSize];
 * when the page on screen needs more pixels than that, only the part inside the viewport is
 * rendered again at full resolution and drawn on top.
 */
internal object TileMath {
    /** Page size in screen pixels at [scale], without the [RenderSize] cap. */
    fun fullSize(
        pageWidth: Double,
        pageHeight: Double,
        scale: Float,
        density: Float,
    ): PixelSize =
        PixelSize(
            max(1, (pageWidth * scale * density).roundToInt()),
            max(1, (pageHeight * scale * density).roundToInt()),
        )

    /** Whether the capped whole-page bitmap is noticeably less sharp than the screen. */
    fun needsDetail(
        full: PixelSize,
        capped: PixelSize,
    ): Boolean = full.width > capped.width * DETAIL_THRESHOLD

    /**
     * The visible part of a page, in pixels of the full-size page. [pageLeft]/[pageTop] is the page's
     * top-left corner and the viewport edges are in the same (window) coordinates. Null when the
     * page is not visible.
     */
    fun visibleRegion(
        full: PixelSize,
        pageLeft: Float,
        pageTop: Float,
        viewportLeft: Float,
        viewportTop: Float,
        viewportRight: Float,
        viewportBottom: Float,
    ): PixelRect? {
        val left = max(0, floor(viewportLeft - pageLeft).toInt())
        val top = max(0, floor(viewportTop - pageTop).toInt())
        val right = min(full.width, ceil(viewportRight - pageLeft).toInt())
        val bottom = min(full.height, ceil(viewportBottom - pageTop).toInt())
        if (right <= left || bottom <= top) return null
        return PixelRect(left, top, right, bottom)
    }

    /** Only redraw the detail when it is at least 5% sharper than the whole-page bitmap. */
    private const val DETAIL_THRESHOLD = 1.05
}
