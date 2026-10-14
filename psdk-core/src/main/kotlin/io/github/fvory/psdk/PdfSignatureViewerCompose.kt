package io.github.fvory.psdk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.ui.ViewerRoot

/**
 * Creates a [PdfSignatureViewer] that survives configuration changes such as rotation.
 *
 * The viewer lives in a ViewModel of the nearest ViewModelStoreOwner (Activity, Fragment or
 * navigation entry) and is destroyed with it. [config] is only used the first time; later changes
 * go through `updateConfig` (milestone M3). Use different [key]s for several viewers on one screen.
 */
@Composable
public fun rememberPdfSignatureViewer(
    config: PsdkConfig = PsdkConfig(),
    key: String = "default",
): PdfSignatureViewer {
    val context = LocalContext.current
    val owner = LocalViewModelStoreOwner.current
    if (owner == null) {
        val viewer = remember(key) { PdfSignatureViewer(context, config) }
        DisposableEffect(viewer) { onDispose { viewer.destroy() } }
        return viewer
    }
    val holder =
        viewModel(viewModelStoreOwner = owner, key = "psdk-viewer-$key") {
            ViewerHolder(PdfSignatureViewer(context.applicationContext, config))
        }
    return holder.viewer
}

/** Shows [viewer]: the pages, or an empty state when no document is loaded. */
@Composable
public fun PdfSignatureViewer(
    viewer: PdfSignatureViewer,
    modifier: Modifier = Modifier,
) {
    ViewerRoot(viewer, modifier)
}

internal class ViewerHolder(
    val viewer: PdfSignatureViewer,
) : ViewModel() {
    override fun onCleared() {
        viewer.destroy()
    }
}
