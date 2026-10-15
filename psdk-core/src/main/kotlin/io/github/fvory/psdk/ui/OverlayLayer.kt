package io.github.fvory.psdk.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.core.GlobalLocks
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
    val colors = LocalPsdkColors.current
    val textMeasurer = rememberTextMeasurer()
    var preview by remember { mutableStateOf<Preview?>(null) }
    val currentItems by rememberUpdatedState(items)
    val config by viewer.configFlow.collectAsState()
    val locks = GlobalLocks(config.disabled, config.disableDragging, config.disableDrag, config.disableResize)
    val keepStampRatio = config.estampKeepAspectRatio
    val useHaptics = config.ui.haptics

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
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
                    colors = colors,
                )
            }
            // Disabled viewer: fade the items (B-LCK-05). Drawn per item instead of a page-sized
            // graphicsLayer, which would exceed the GPU texture limit at high zoom.
            if (locks.disabled) {
                val pxPerPoint = scale * density
                val margin = HitTester.DELETE_RADIUS_DP.dp.toPx()
                for (item in items) {
                    drawRect(
                        Color.White.copy(alpha = 0.5f),
                        topLeft = Offset((item.x * pxPerPoint).toFloat() - margin, (item.y * pxPerPoint).toFloat() - margin),
                        size = Size((item.width * pxPerPoint).toFloat() + 2 * margin, (item.height * pxPerPoint).toFloat() + 2 * margin),
                    )
                }
            }
        }
        // Invisible accessibility nodes on top of the drawn items (B-A11Y-02). They have no pointer
        // input, so touches still reach the canvas.
        for (item in items) ItemAccessibilityNode(viewer, item, scale, locks, config)
    }
}

/** TalkBack element for one item: "Direktur, page 2" with move, resize and delete actions. */
@Composable
private fun ItemAccessibilityNode(
    viewer: PdfSignatureViewer,
    item: SignatureItem,
    scale: Float,
    locks: GlobalLocks,
    config: io.github.fvory.psdk.config.PsdkConfig,
) {
    val labels = config.labels
    val description = labels.a11yItem.format(item.label, item.page)
    val actions =
        buildList {
            if (LockRules.canDrag(locks, item)) {
                add(CustomAccessibilityAction(labels.a11yMoveUp) { viewer.nudgeItem(item.id, 0.0, -A11Y_STEP).let { true } })
                add(CustomAccessibilityAction(labels.a11yMoveDown) { viewer.nudgeItem(item.id, 0.0, A11Y_STEP).let { true } })
                add(CustomAccessibilityAction(labels.a11yMoveLeft) { viewer.nudgeItem(item.id, -A11Y_STEP, 0.0).let { true } })
                add(CustomAccessibilityAction(labels.a11yMoveRight) { viewer.nudgeItem(item.id, A11Y_STEP, 0.0).let { true } })
            }
            if (LockRules.canResize(locks, item)) {
                add(CustomAccessibilityAction(labels.a11yBigger) { viewer.scaleItem(item.id, 1.1).let { true } })
                add(CustomAccessibilityAction(labels.a11ySmaller) { viewer.scaleItem(item.id, 0.9).let { true } })
            }
            if (LockRules.canDelete(locks)) {
                add(CustomAccessibilityAction(labels.a11yDelete) { viewer.removeSignature(item.id).let { true } })
            }
        }
    Box(
        Modifier
            .offset((item.x * scale).dp, (item.y * scale).dp)
            .size((item.width * scale).dp, (item.height * scale).dp)
            // Keep the semantic target usable when the item is zoomed out. This does not
            // change the drawn overlay; it only guarantees the minimum Android touch target.
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                contentDescription = description
                customActions = actions
            },
    )
}

/** Distance of one accessibility "move" step, in PDF points. */
private const val A11Y_STEP = 10.0
