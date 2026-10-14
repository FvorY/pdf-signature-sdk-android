package io.github.fvory.psdk.core

import kotlin.math.max
import kotlin.math.min

/** A page currently laid out by the list, in pixels relative to the list viewport. */
internal data class VisiblePage(
    val page: Int,
    val top: Int,
    val bottom: Int,
)

/** Picks the active page while scrolling. */
internal object ActivePageDetector {
    /**
     * The page with the tallest visible part (B-PG-04).
     *
     * Mobile addition: once the user has scrolled to the very end, the last page becomes active
     * even if it is shorter than the one above it (e.g. a landscape last page). Otherwise a short
     * last page could never become active. When nothing can scroll, the tallest-part rule applies.
     */
    fun pick(
        visible: List<VisiblePage>,
        viewportTop: Int,
        viewportBottom: Int,
        current: Int,
        canScrollBackward: Boolean = false,
        canScrollForward: Boolean = true,
    ): Int {
        if (visible.isEmpty()) return current
        if (canScrollBackward && !canScrollForward) return visible.maxOf { it.page }

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
