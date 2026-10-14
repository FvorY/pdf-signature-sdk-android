package io.github.fvory.psdk.ui

import android.os.SystemClock
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.PsdkState
import io.github.fvory.psdk.ViewerCommand
import io.github.fvory.psdk.core.ActivePageDetector
import io.github.fvory.psdk.core.VisiblePage
import io.github.fvory.psdk.core.ZoomMath
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PAGE_GAP = 12.dp
private val VERTICAL_PADDING = 16.dp

/** Settle time after a gesture zoom, like `setScale` (B-ZM-01). */
private const val ZOOM_SETTLE_MILLIS = 850L

/**
 * Vertical list of pages (lazy, spec §15.1 R1) that also scrolls horizontally when a page is wider
 * than the screen. Handles `goToPage`/`setScale` scrolling, active-page detection and gesture zoom.
 */
@Composable
internal fun PageList(
    viewer: PdfSignatureViewer,
    state: PsdkState,
) {
    val listState = remember(state.documentId) { LazyListState() }
    val horizontalScroll = remember(state.documentId) { ScrollState(0) }
    val scope = rememberCoroutineScope()
    var programmaticScroll by remember { mutableStateOf(false) }
    var suppressDetectionUntil by remember { mutableLongStateOf(0L) }
    var pinching by remember { mutableStateOf(false) }
    var pinchZoom by remember { mutableFloatStateOf(1f) }
    var pinchCentroid by remember { mutableStateOf(Offset.Zero) }
    val scale = state.scale

    /** Applies [target] and scrolls so the content under [focal] (px in this view) stays there. */
    suspend fun zoomAround(
        target: Float,
        focal: Offset,
    ) {
        val clamped = ZoomMath.clamp(target)
        if (clamped == scale) return
        val ratio = clamped / scale
        val firstIndex = listState.firstVisibleItemIndex
        val verticalOffset = ((listState.firstVisibleItemScrollOffset + focal.y) * ratio - focal.y).roundToInt()
        val horizontalOffset = ((horizontalScroll.value + focal.x) * ratio - focal.x).roundToInt()
        suppressDetectionUntil = SystemClock.uptimeMillis() + ZOOM_SETTLE_MILLIS
        viewer.applyGestureScale(clamped)
        withFrameNanos { }
        listState.scrollToItem(firstIndex)
        listState.scrollBy(verticalOffset.toFloat())
        horizontalScroll.scrollTo(horizontalOffset.coerceIn(0, horizontalScroll.maxValue))
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        LaunchedEffect(viewportWidth, viewportHeight) {
            viewer.onViewportChanged(viewportWidth.value, viewportHeight.value)
        }

        val widestPage = state.pageSizes.maxOf { it.width }
        val contentWidth =
            max(viewportWidth, (widestPage * scale).dp + (PdfSignatureViewer.PAGE_HORIZONTAL_PADDING_DP * 2).dp)

        // goToPage / setScale requests from the viewer (B-PG-02, B-ZM-01)
        LaunchedEffect(viewer, listState) {
            viewer.commands.collectLatest { command ->
                when (command) {
                    is ViewerCommand.ScrollToPage -> {
                        programmaticScroll = true
                        try {
                            if (command.animate) {
                                listState.animateScrollToItem(command.page - 1)
                            } else {
                                withFrameNanos { }
                                listState.scrollToItem(command.page - 1)
                            }
                        } finally {
                            programmaticScroll = false
                            suppressDetectionUntil = SystemClock.uptimeMillis() + command.settleMillis
                        }
                    }
                }
            }
        }

        // Active page while the user scrolls (B-PG-04, B-PG-05)
        LaunchedEffect(viewer, listState) {
            snapshotFlow { listState.layoutInfo }.collect { info ->
                if (programmaticScroll || pinching || SystemClock.uptimeMillis() < suppressDetectionUntil) return@collect
                val visible = info.visibleItemsInfo.map { VisiblePage(it.index + 1, it.offset, it.offset + it.size) }
                val page = ActivePageDetector.pick(visible, info.viewportStartOffset, info.viewportEndOffset, viewer.currentPage)
                viewer.onScrolledToPage(page)
            }
        }

        val zoomGestures =
            if (viewer.config.ui.viewer.zoom) {
                Modifier
                    .pinchToZoom(
                        key = scale,
                        onZoom = { zoom, centroid ->
                            pinching = true
                            pinchZoom = zoom.coerceIn(ZoomMath.MIN_SCALE / scale, ZoomMath.MAX_SCALE / scale)
                            pinchCentroid = centroid
                        },
                        onEnd = { zoom, centroid ->
                            val target = scale * zoom
                            scope.launch {
                                try {
                                    zoomAround(target, centroid)
                                } finally {
                                    pinchZoom = 1f
                                    pinching = false
                                }
                            }
                        },
                    ).pointerInput(scale) {
                        // Double tap: 2× fit-width, or back to fit-width (spec §14.3)
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val fit = viewer.fitToWidthScale() ?: return@detectTapGestures
                                val target = if (scale < fit * 2 - 0.01f) fit * 2 else fit
                                scope.launch { zoomAround(target, offset) }
                            },
                        )
                    }
            } else {
                Modifier
            }

        Box(
            Modifier
                .fillMaxSize()
                .then(zoomGestures)
                .graphicsLayer {
                    scaleX = pinchZoom
                    scaleY = pinchZoom
                    transformOrigin =
                        if (size.width > 0f && size.height > 0f) {
                            TransformOrigin(pinchCentroid.x / size.width, pinchCentroid.y / size.height)
                        } else {
                            TransformOrigin.Center
                        }
                }.horizontalScroll(horizontalScroll),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(contentWidth).fillMaxHeight(),
                contentPadding = PaddingValues(vertical = VERTICAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(PAGE_GAP),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(count = state.totalPages, key = { it }) { index ->
                    PageItem(viewer, index + 1, state.pageSizes[index], scale, state.documentId)
                }
            }
        }
    }
}
