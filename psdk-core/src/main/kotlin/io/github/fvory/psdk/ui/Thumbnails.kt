package io.github.fvory.psdk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.PsdkState
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.engine.RenderSize
import kotlin.math.max

private val STRIP_HEIGHT = 108.dp
private val STRIP_THUMB_HEIGHT = 64.dp
private val SIDEBAR_WIDTH = 190.dp
private val SIDEBAR_THUMB_WIDTH = 140.dp

/** Phone: a horizontal strip of page thumbnails above the bottom bar (spec §13.2). */
@Composable
internal fun ThumbnailStrip(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
) {
    val colors = LocalPsdkColors.current
    val listState = rememberLazyListState()
    FollowActivePage(listState, state.currentPage)
    HorizontalDivider(color = colors.border)
    LazyRow(
        state = listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(STRIP_HEIGHT)
                .background(colors.surface)
                .thumbnailsEnabled(state, config),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(count = state.totalPages, key = { it }) { index ->
            val size = state.pageSizes[index]
            val height = STRIP_THUMB_HEIGHT
            val width = height * (size.width / size.height).toFloat()
            Thumbnail(viewer, state, config, index + 1, width, height)
        }
    }
}

/** Tablet: left sidebar with a "Pages" header and page count (B-TH-01). */
@Composable
internal fun ThumbnailSidebar(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
) {
    val colors = LocalPsdkColors.current
    val listState = rememberLazyListState()
    FollowActivePage(listState, state.currentPage)
    Row(Modifier.fillMaxHeight()) {
        Column(Modifier.width(SIDEBAR_WIDTH).fillMaxHeight().background(colors.surface)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    config.labels.sidebarTitle.uppercase(),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${state.totalPages}",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            HorizontalDivider(color = colors.border)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().thumbnailsEnabled(state, config),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(count = state.totalPages, key = { it }) { index ->
                    val size = state.pageSizes[index]
                    val width = SIDEBAR_THUMB_WIDTH
                    val height = width * (size.height / size.width).toFloat()
                    Thumbnail(viewer, state, config, index + 1, width, height)
                }
            }
        }
        VerticalDivider(color = colors.border)
    }
}

/** B-PG-06, B-LCK-05: dimmed and not clickable while pagination is locked or the viewer is disabled. */
private fun Modifier.thumbnailsEnabled(
    state: PsdkState,
    config: PsdkConfig,
): Modifier = graphicsLayer { alpha = if (state.isPaginationLocked || config.disabled) 0.5f else 1f }

/** Keeps the active thumbnail in view (B-PG-09). */
@Composable
private fun FollowActivePage(
    listState: LazyListState,
    page: Int,
) {
    LaunchedEffect(page) {
        val index = page - 1
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (index !in visible.drop(1).dropLast(1)) listState.animateScrollToItem(max(0, index - 1))
    }
}

@Composable
private fun Thumbnail(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    page: Int,
    width: Dp,
    height: Dp,
) {
    val colors = LocalPsdkColors.current
    val active = page == state.currentPage
    val clickable = !state.isPaginationLocked && !config.disabled
    val density = LocalDensity.current.density
    val size = state.pageSizes[page - 1]
    var bitmap by remember(state.documentId, page) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(state.documentId, page, width) {
        val pixels = RenderSize.compute(size.width, size.height, width.value / size.width.toFloat(), density)
        viewer.renderForDisplay(page, pixels.width, pixels.height)?.let { bitmap = it.asImageBitmap() }
    }
    val label = config.labels.thumbLabel.format(page)
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) colors.accentSubtle else Color.Transparent)
            .border(1.dp, if (active) colors.accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(enabled = clickable, role = Role.Button) { viewer.goToPage(page) }
            .semantics {
                contentDescription = label
                selected = active
            }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(width, height).background(Color.White).border(1.dp, colors.border)) {
            bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
        }
        Text(
            label,
            color = if (active) colors.accent else colors.textMuted,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
