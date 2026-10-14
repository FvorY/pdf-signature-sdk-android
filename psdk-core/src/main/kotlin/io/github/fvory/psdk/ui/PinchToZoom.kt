package io.github.fvory.psdk.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Two-finger zoom that leaves one-finger scrolling to the content (spec §14.2).
 *
 * Events are read in the Initial pass, before the scrolling list sees them, and consumed only
 * while two or more fingers are down. [onZoom] gets the total zoom factor since the gesture
 * started and the centroid in this element's pixels; [onEnd] gets the final values.
 */
internal fun Modifier.pinchToZoom(
    key: Any?,
    onZoom: (zoom: Float, centroid: Offset) -> Unit,
    onEnd: (zoom: Float, centroid: Offset) -> Unit,
): Modifier =
    pointerInput(key) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var zoom = 1f
            var centroid = Offset.Zero
            var zooming = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.count { it.pressed }
                if (pressed == 0) break
                if (pressed >= 2) {
                    zooming = true
                    zoom *= event.calculateZoom()
                    centroid = event.calculateCentroid(useCurrent = true)
                    onZoom(zoom, centroid)
                    event.changes.forEach { it.consume() }
                }
            }
            if (zooming) onEnd(zoom, centroid)
        }
    }
