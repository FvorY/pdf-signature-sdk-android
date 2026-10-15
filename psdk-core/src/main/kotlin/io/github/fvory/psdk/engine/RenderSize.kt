package io.github.fvory.psdk.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class PixelSize(
    val width: Int,
    val height: Int,
)

/**
 * Bitmap size for a whole page (spec §15.1 R2): page points × scale × density, capped so the
 * longest side is at most [MAX_SIDE] px and the bitmap has at most [MAX_PIXELS] pixels (16 MB).
 * Beyond the cap the visible part is rendered sharp by [TileMath], so the cap only bounds memory:
 * with 16 MP pages a 300-page document used about 450 MB at 400% zoom on a 1080p phone.
 */
internal object RenderSize {
    const val MAX_SIDE: Int = 3072
    const val MAX_PIXELS: Int = 4_000_000

    fun compute(
        pageWidth: Double,
        pageHeight: Double,
        scale: Float,
        density: Float,
    ): PixelSize {
        val width = pageWidth * scale * density
        val height = pageHeight * scale * density
        val factor =
            min(
                1.0,
                min(MAX_SIDE / max(width, height), sqrt(MAX_PIXELS / (width * height))),
            )
        return PixelSize(
            width = max(1, (width * factor).roundToInt()),
            height = max(1, (height * factor).roundToInt()),
        )
    }
}
