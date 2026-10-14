package io.github.fvory.psdk.core

/**
 * Converts between PDF points and on-screen dp (spec §5.3).
 * [origin] is the page's left (or top) edge on screen, in dp.
 */
internal object CoordinateMapper {
    fun pointToScreen(
        point: Double,
        scale: Float,
        origin: Float = 0f,
    ): Float = origin + (point * scale).toFloat()

    fun screenToPoint(
        screen: Float,
        scale: Float,
        origin: Float = 0f,
    ): Double = (screen - origin).toDouble() / scale
}
