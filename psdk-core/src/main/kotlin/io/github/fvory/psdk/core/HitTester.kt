package io.github.fvory.psdk.core

import io.github.fvory.psdk.model.SignatureItem
import kotlin.math.abs
import kotlin.math.hypot

internal enum class HitPart { DELETE, RESIZE, BODY }

internal data class Hit(
    val item: SignatureItem,
    val part: HitPart,
)

/**
 * Finds what a touch hits (B-INT-01/02). Touch areas are fixed in dp so they stay usable at any
 * zoom (spec §14.1): delete = 24 dp around the top-right corner, resize = 48 dp square around
 * the bottom-right corner.
 */
internal object HitTester {
    const val DELETE_RADIUS_DP: Float = 24f
    const val RESIZE_HALF_DP: Float = 24f

    /**
     * [items] are the items of one page in creation order (last = on top).
     * [x], [y] are the touch position in dp from the page's top-left corner.
     */
    fun hit(
        items: List<SignatureItem>,
        x: Float,
        y: Float,
        scale: Float,
        locks: GlobalLocks,
    ): Hit? {
        if (locks.disabled) return null
        for (item in items.asReversed()) {
            val left = (item.x * scale).toFloat()
            val top = (item.y * scale).toFloat()
            val right = left + (item.width * scale).toFloat()
            val bottom = top + (item.height * scale).toFloat()

            if (hypot(x - right, y - top) <= DELETE_RADIUS_DP) return Hit(item, HitPart.DELETE)
            if (LockRules.canResize(locks, item) && abs(x - right) <= RESIZE_HALF_DP && abs(y - bottom) <= RESIZE_HALF_DP) {
                return Hit(item, HitPart.RESIZE)
            }
            if (LockRules.canDrag(locks, item) && x in left..right && y in top..bottom) return Hit(item, HitPart.BODY)
        }
        return null
    }
}
