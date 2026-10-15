package io.github.fvory.psdk.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.engine.PixelRect
import io.github.fvory.psdk.engine.PixelSize
import io.github.fvory.psdk.engine.RenderSize
import io.github.fvory.psdk.engine.TileMath
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.SignatureItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

/**
 * One page: a white sheet sized to the page in points × scale, with the rendered bitmap on top.
 * After a zoom the previous bitmap stays visible (stretched) until the sharp one is ready (R7).
 * At high zoom the visible part is rendered again at full resolution on top ([TileMath]).
 * The page's signatures and E-Materai are drawn on top by [OverlayLayer].
 */
@Composable
internal fun PageItem(
    viewer: PdfSignatureViewer,
    page: Int,
    size: PageSize,
    scale: Float,
    documentId: Long,
    items: List<SignatureItem>,
    itemBitmaps: Map<String, Bitmap>,
    viewport: State<Rect?>,
    detailAllowed: Boolean,
) {
    val density = LocalDensity.current.density
    val pixels = remember(size, scale, density) { RenderSize.compute(size.width, size.height, scale, density) }
    var bitmap by remember(documentId, page) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(documentId, page, pixels) {
        viewer.renderForDisplay(page, pixels.width, pixels.height)?.let { bitmap = it.asImageBitmap() }
    }

    // Sharp detail at high zoom: re-render only the visible part once scrolling settles
    val full = remember(size, scale, density) { TileMath.fullSize(size.width, size.height, scale, density) }
    val needsDetail = remember(full, pixels) { TileMath.needsDetail(full, pixels) }
    var pagePosition by remember { mutableStateOf<Offset?>(null) }
    var detail by remember(documentId, page) { mutableStateOf<Detail?>(null) }
    LaunchedEffect(documentId, page, full, needsDetail, detailAllowed) {
        if (!needsDetail || !detailAllowed) {
            detail = null
            return@LaunchedEffect
        }
        snapshotFlow { pagePosition to viewport.value }
            .debounce(DETAIL_DELAY_MS)
            .collectLatest { (position, window) ->
                if (position == null || window == null) return@collectLatest
                val region =
                    TileMath.visibleRegion(full, position.x, position.y, window.left, window.top, window.right, window.bottom)
                        ?: return@collectLatest
                if (detail?.let { it.full == full && it.region == region } == true) return@collectLatest
                viewer.renderDetail(page, full, region)?.let { detail = Detail(full, region, it.asImageBitmap()) }
            }
    }

    val description = viewer.labels.a11yPage.format(page, viewer.totalPages)
    Box(
        Modifier
            .size((size.width * scale).dp, (size.height * scale).dp)
            // No Modifier.shadow: it adds a layer as big as the page, and at high zoom that layer is
            // larger than the GPU texture limit and the whole page disappears.
            .drawBehind { drawPageShadow() }
            .background(Color.White)
            .semantics { contentDescription = description }
            .onGloballyPositioned { pagePosition = it.positionInWindow() },
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Medium,
            )
        }
        detail?.takeIf { it.full == full }?.let { shown ->
            with(LocalDensity.current) {
                Image(
                    bitmap = shown.image,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .offset { IntOffset(shown.region.left, shown.region.top) }
                            .size(shown.region.width.toDp(), shown.region.height.toDp()),
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.None,
                )
            }
        }
        OverlayLayer(viewer, page, size, scale, items, itemBitmaps)
    }
}

/** A full-resolution render of [region] of the page drawn at [full] size. */
private class Detail(
    val full: PixelSize,
    val region: PixelRect,
    val image: ImageBitmap,
)

private const val DETAIL_DELAY_MS = 150L

/** A thin edge and a soft line under the page, drawn without an offscreen layer. */
private fun DrawScope.drawPageShadow() {
    val edge = 1.dp.toPx()
    drawRect(Color.Black.copy(alpha = 0.06f), topLeft = Offset(-edge, -edge), size = Size(size.width + 2 * edge, size.height + 2 * edge))
    drawRect(Color.Black.copy(alpha = 0.10f), topLeft = Offset(0f, size.height), size = Size(size.width, 2 * edge))
}
