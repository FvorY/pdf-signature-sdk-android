package io.github.fvory.psdk.core

import kotlin.math.max
import kotlin.math.min

internal data class ItemBox(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

/** Drag and resize math in PDF points, following the web SDK (B-INT-04…06). */
internal object ItemGeometry {
    /** Smallest width/height while resizing. The web SDK uses 30 canvas px (20 pt at its default 1.5 zoom). */
    const val MIN_SIZE: Double = 20.0

    /** New top-left corner: pointer minus where the item was grabbed, kept inside the page. */
    fun drag(
        box: ItemBox,
        pointerX: Double,
        pointerY: Double,
        grabX: Double,
        grabY: Double,
        pageWidth: Double,
        pageHeight: Double,
    ): ItemBox =
        box.copy(
            x = max(0.0, min(pageWidth - box.width, pointerX - grabX)),
            y = max(0.0, min(pageHeight - box.height, pointerY - grabY)),
        )

    /** Accessibility "move" action: shift by [dx], [dy] points, kept inside the page (B-A11Y-02). */
    fun nudge(
        box: ItemBox,
        dx: Double,
        dy: Double,
        pageWidth: Double,
        pageHeight: Double,
    ): ItemBox = drag(box, box.x + dx, box.y + dy, 0.0, 0.0, pageWidth, pageHeight)

    /**
     * Accessibility "bigger/smaller" action: scale by [factor] from the top-left corner, with the same
     * limits as the resize handle. The aspect ratio is kept.
     */
    fun scaleBy(
        box: ItemBox,
        factor: Double,
        pageWidth: Double,
        pageHeight: Double,
    ): ItemBox = resize(box, box.width * (factor - 1), box.height * (factor - 1), pageWidth, pageHeight, keepAspectRatio = true)

    /**
     * New size when dragging the bottom-right handle by [dx], [dy] from the size at gesture start.
     * Width and height change independently like the web SDK, unless [keepAspectRatio] is set.
     */
    fun resize(
        start: ItemBox,
        dx: Double,
        dy: Double,
        pageWidth: Double,
        pageHeight: Double,
        keepAspectRatio: Boolean = false,
    ): ItemBox {
        val maxWidth = pageWidth - start.x
        val maxHeight = pageHeight - start.y
        if (!keepAspectRatio) {
            return start.copy(
                width = min(maxWidth, max(MIN_SIZE, start.width + dx)),
                height = min(maxHeight, max(MIN_SIZE, start.height + dy)),
            )
        }
        val ratio = start.height / start.width
        var width = min(maxWidth, max(MIN_SIZE, start.width + dx))
        var height = width * ratio
        if (height > maxHeight) {
            height = maxHeight
            width = height / ratio
        }
        return start.copy(width = width, height = height)
    }
}
