package io.github.fvory.psdk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.engine.RenderSize
import io.github.fvory.psdk.model.PageSize

/**
 * One page: a white sheet sized to the page in points × scale, with the rendered bitmap on top.
 * After a zoom the previous bitmap stays visible (stretched) until the sharp one is ready (R7).
 */
@Composable
internal fun PageItem(
    viewer: PdfSignatureViewer,
    page: Int,
    size: PageSize,
    scale: Float,
    documentId: Long,
) {
    val density = LocalDensity.current.density
    val pixels = remember(size, scale, density) { RenderSize.compute(size.width, size.height, scale, density) }
    var bitmap by remember(documentId, page) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(documentId, page, pixels) {
        viewer.renderForDisplay(page, pixels.width, pixels.height)?.let { bitmap = it.asImageBitmap() }
    }

    val description = viewer.labels.a11yPage.format(page, viewer.totalPages)
    Box(
        Modifier
            .size((size.width * scale).dp, (size.height * scale).dp)
            .shadow(2.dp)
            .background(Color.White)
            .semantics { contentDescription = description },
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
    }
}
