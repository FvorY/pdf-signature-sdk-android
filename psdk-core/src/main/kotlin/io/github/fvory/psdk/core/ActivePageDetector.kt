package io.github.fvory.psdk.core

import kotlin.math.max
import kotlin.math.min

/** A page currently laid out by the list, in pixels relative to the list viewport. */
internal data class VisiblePage(
    val page: Int,
    val top: Int,
    val bottom: Int,
)

/** Picks the active page while scrolling: the one with the tallest visible part (B-PG-04). */
internal object ActivePageDetector {
    fun pick(
        visible: List<VisiblePage>,
        viewportTop: Int,
        viewportBottom: Int,
        current: Int,
    ): Int {
        var best = current
        var bestHeight = 0
        for (page in visible) {
            val height = min(page.bottom, viewportBottom) - max(page.top, viewportTop)
            if (height > bestHeight) {
                bestHeight = height
                best = page.page
            }
        }
        return best
    }
}
