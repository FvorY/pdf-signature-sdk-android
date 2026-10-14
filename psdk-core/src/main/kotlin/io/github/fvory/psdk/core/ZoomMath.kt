package io.github.fvory.psdk.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Zoom rules from the web SDK (B-ZM-01…04). A scale of 1 draws one PDF point as one dp. */
internal object ZoomMath {
    const val MIN_SCALE: Float = 0.25f
    const val MAX_SCALE: Float = 4f
    const val STEP: Float = 0.25f

    /** Space the web SDK leaves around the page in "fit to screen", in dp. */
    const val FIT_PADDING: Float = 48f

    fun clamp(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /** Zoom-in button: +0.25 rounded to 2 decimals; no change at the maximum. */
    fun stepUp(scale: Float): Float = if (scale >= MAX_SCALE) scale else min(MAX_SCALE, round2(scale + STEP))

    /** Zoom-out button: −0.25 rounded to 2 decimals; no change at the minimum. */
    fun stepDown(scale: Float): Float = if (scale <= MIN_SCALE) scale else max(MIN_SCALE, round2(scale - STEP))

    /** Whole page visible: viewport minus 48 on each axis (B-ZM-04). */
    fun fitToScreen(
        viewportWidth: Float,
        viewportHeight: Float,
        pageWidth: Double,
        pageHeight: Double,
    ): Float {
        val byWidth = (viewportWidth - FIT_PADDING) / pageWidth
        val byHeight = (viewportHeight - FIT_PADDING) / pageHeight
        return clamp(min(byWidth, byHeight).toFloat())
    }

    /** Page as wide as the viewport minus [horizontalPadding] on both sides. */
    fun fitToWidth(
        viewportWidth: Float,
        pageWidth: Double,
        horizontalPadding: Float,
    ): Float = clamp(((viewportWidth - 2 * horizontalPadding) / pageWidth).toFloat())

    /** Percentage label shown next to the zoom buttons (B-ZM-03). */
    fun percentLabel(scale: Float): String = "${(scale * 100).roundToInt()}%"

    private fun round2(value: Float): Float = (value * 100).roundToInt() / 100f
}
