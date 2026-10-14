package io.github.fvory.psdk.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.model.PsdkImage

/** Shows a [PsdkImage] (logo, picker preview), loaded through the viewer's image cache. */
@Composable
internal fun PsdkImageView(
    viewer: PdfSignatureViewer,
    image: PsdkImage,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val bitmap by produceState<ImageBitmap?>(null, image) { value = viewer.loadImage(image)?.asImageBitmap() }
    bitmap?.let { Image(it, contentDescription = null, modifier = modifier, contentScale = contentScale) }
}
