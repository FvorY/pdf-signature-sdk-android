package io.github.fvory.psdk.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.core.HitPart
import io.github.fvory.psdk.core.HitTester
import io.github.fvory.psdk.core.ItemBox
import io.github.fvory.psdk.core.ItemGeometry
import io.github.fvory.psdk.core.LockRules
import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.SignatureItem
import io.github.fvory.psdk.ui.ItemPainter.drawItem

private data class Preview(
    val id: String,
    val box: ItemBox,
)

/**
 * Items of one page and their gestures (B-INT-01…09, spec §14.2).
 *
 * A touch that hits an item is consumed so the page list does not scroll; any other touch is left
 * to the list. While dragging, the item moves locally ([Preview]); the store is updated and
 * `signatureMoved` is sent only when the finger lifts. A second finger cancels the drag.
 */
@Composable
internal fun OverlayLayer(
    viewer: PdfSignatureViewer,
    page: Int,
    pageSize: PageSize,
    scale: Float,
    items: List<SignatureItem>,
    bitmaps: Map<String, Bitmap>,
) {
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    var preview by remember { mutableStateOf<Preview?>(null) }
    val currentItems by rememberUpdatedState(items)
    val locks = viewer.locks
    val keepStampRatio = viewer.config.estampKeepAspectRatio
    val useHaptics = viewer.config.ui.haptics

    Canvas(
        Modifier
            .fillMaxSize()
            .alpha(if (locks.disabled) 0.5f else 1f)
            .pointerInput(page, scale, locks, keepStampRatio, pageSize) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val pxPerPoint = scale * density
                    val hit =
                        HitTester.hit(
                            items = currentItems,
                            x = down.position.x / density,
                            y = down.position.y / density,
                            scale = scale,
                            locks = locks,
                        ) ?: return@awaitEachGesture
                    down.consume()

                    val item = hit.item
                    val start = ItemBox(item.x, item.y, item.width, item.height)
                    val grabX = down.position.x / pxPerPoint - item.x
                    val grabY = down.position.y / pxPerPoint - item.y
                    var box = start
                    var moved = false
                    var cancelled = false

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) {
                            cancelled = true
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.position - down.position
                        if (!moved && delta.getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                            if (useHaptics && hit.part != HitPart.DELETE) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (moved) {
                            box =
                                when (hit.part) {
                                    HitPart.BODY ->
                                        ItemGeometry.drag(
                                            start,
                                            pointerX = (change.position.x / pxPerPoint).toDouble(),
                                            pointerY = (change.position.y / pxPerPoint).toDouble(),
                                            grabX = grabX,
                                            grabY = grabY,
                                            pageWidth = pageSize.width,
                                            pageHeight = pageSize.height,
                                        )
                                    HitPart.RESIZE ->
                                        ItemGeometry.resize(
                                            start,
                                            dx = (delta.x / pxPerPoint).toDouble(),
                                            dy = (delta.y / pxPerPoint).toDouble(),
                                            pageWidth = pageSize.width,
                                            pageHeight = pageSize.height,
                                            keepAspectRatio = keepStampRatio && item.type == ItemType.ESTAMP,
                                        )
                                    HitPart.DELETE -> box
                                }
                            if (hit.part != HitPart.DELETE) preview = Preview(item.id, box)
                        }
                        change.consume()
                    }

                    when {
                        cancelled -> Unit
                        hit.part == HitPart.DELETE && !moved -> {
                            if (useHaptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewer.removeSignature(item.id)
                        }
                        moved && hit.part != HitPart.DELETE -> viewer.commitItemBox(item.id, box)
                    }
                    preview = null
                }
            },
    ) {
        val active = preview
        for (item in items) {
            val shown =
                if (active?.id == item.id) {
                    item.copy(x = active.box.x, y = active.box.y, width = active.box.width, height = active.box.height)
                } else {
                    item
                }
            drawItem(
                item = shown,
                image = bitmaps[item.id]?.asImageBitmap(),
                scale = scale,
                canDrag = LockRules.canDrag(locks, item),
                canResize = LockRules.canResize(locks, item),
                active = active?.id == item.id,
                textMeasurer = textMeasurer,
            )
        }
    }
}
