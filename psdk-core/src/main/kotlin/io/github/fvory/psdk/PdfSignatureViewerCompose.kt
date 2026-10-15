package io.github.fvory.psdk

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.state.ViewerSnapshot
import io.github.fvory.psdk.ui.ViewerRoot

/**
 * Creates a [PdfSignatureViewer] that survives configuration changes such as rotation, and comes
 * back with the same document, page, zoom and items after Android kills the app in the background.
 * Password-protected documents ask for the password again; passwords are never saved.
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
            val handle = createSavedStateHandle()
            ViewerHolder(handle) { snapshot -> PdfSignatureViewer(context.applicationContext, config, snapshot) }
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

/**
 * Keeps the viewer across rotation, and saves a [ViewerSnapshot] so the document, page, zoom and
 * items come back after Android kills the app in the background (spec §17).
 */
internal class ViewerHolder(
    handle: SavedStateHandle,
    create: (ViewerSnapshot?) -> PdfSignatureViewer,
) : ViewModel() {
    val viewer: PdfSignatureViewer = create(ViewerSnapshot.fromJson(handle.get<Bundle>(PROVIDER)?.getString(KEY)))

    init {
        handle.setSavedStateProvider(PROVIDER) { bundleOf(KEY to viewer.snapshot().toJson()) }
    }

    override fun onCleared() {
        viewer.destroy()
    }

    private companion object {
        const val PROVIDER = "psdk-viewer"
        const val KEY = "snapshot"
    }
}
