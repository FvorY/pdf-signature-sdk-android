package io.github.fvory.psdk.ui

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.PsdkState
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkLogLevel
import io.github.fvory.psdk.config.SkeletonOptions
import io.github.fvory.psdk.model.PdfSource

/**
 * The whole viewer: toolbars, thumbnails, pages, picker, dialogs and overlays, laid out for phones
 * or tablets (spec §13). Everything follows `viewer.config`, so `updateConfig` applies right away.
 */
@Composable
internal fun ViewerRoot(
    viewer: PdfSignatureViewer,
    modifier: Modifier = Modifier,
) {
    val config by viewer.configFlow.collectAsState()
    SecureScreen(config.secureScreen)
    PsdkTheme(config) {
        val state by viewer.state.collectAsState()
        val picker by viewer.picker.collectAsState()
        val skeleton by viewer.skeleton.collectAsState()
        val passwordPrompt by viewer.passwordPrompt.collectAsState()
        val colors = LocalPsdkColors.current
        val upload = rememberUploadAction(viewer)
        var goToPageOpen by remember { mutableStateOf(false) }

        BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
            val tablet = ChromeRules.isTablet(config.ui.layout, maxWidth.value)
            val iconOnly = maxWidth.value < ChromeRules.ICON_ONLY_MAX_WIDTH_DP
            val viewerHeight = maxHeight.value
            val topbar = config.ui.topbar
            val showToolbars = ChromeRules.topBarHasContent(topbar)
            val showPill = !tablet && topbar.pagination && state.isLoaded

            Column(Modifier.fillMaxSize()) {
                if (showToolbars) {
                    if (tablet) TabletTopBar(viewer, state, config, upload) else PhoneTopBar(viewer, state, config)
                }
                Row(Modifier.weight(1f)) {
                    if (tablet && config.ui.sidebar.thumbnails && state.isLoaded) ThumbnailSidebar(viewer, state, config)
                    ViewerArea(
                        viewer = viewer,
                        state = state,
                        config = config,
                        skeleton = skeleton,
                        upload = upload.takeIf { topbar.upload },
                        showPill = showPill,
                        onOpenGoToPage = { goToPageOpen = true },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (!tablet) {
                    if (config.ui.sidebar.thumbnails && state.isLoaded && ChromeRules.showThumbnailStrip(viewerHeight)) {
                        ThumbnailStrip(viewer, state, config)
                    }
                    if (showToolbars) PhoneBottomBar(viewer, state, config, upload, iconOnly)
                }
                if (config.ui.statusbar && ChromeRules.showStatusBar(viewerHeight)) StatusBar(state, config)
            }

            picker?.let { PickerSheet(viewer, it, config, tablet) }
        }
        if (goToPageOpen) GoToPageDialog(viewer, state, config, onDismiss = { goToPageOpen = false })
        passwordPrompt?.let { PasswordDialog(viewer, it, config.labels) }
    }
}

@Composable
private fun ViewerArea(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    skeleton: SkeletonOptions?,
    upload: (() -> Unit)?,
    showPill: Boolean,
    onOpenGoToPage: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalPsdkColors.current
    Box(modifier.clipToBounds()) {
        val blur = skeleton?.blurRadius
        Box(Modifier.fillMaxSize().then(if (blur != null) Modifier.blur(blur) else Modifier)) {
            when {
                state.isLoaded -> PageList(viewer, state, bottomPadding = if (showPill) 80.dp else 16.dp)
                !state.isLoading ->
                    EmptyState(config.labels, state.error, upload, showErrorDetails = config.logLevel == PsdkLogLevel.DEBUG)
            }
            if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.accent)
        }
        if (showPill) {
            PaginationPill(viewer, state, config, onOpenGoToPage, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
        }
        SkeletonOverlay(skeleton)
    }
}

/**
 * Opens the system file picker for PDFs (B-UPL-01). Null when the host cannot launch activities
 * for results (not inside a ComponentActivity), in which case the upload button is hidden.
 */
@Composable
private fun rememberUploadAction(viewer: PdfSignatureViewer): (() -> Unit)? {
    if (LocalActivityResultRegistryOwner.current == null) return null
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewer.startUpload(PdfSource.UriSource(uri))
        }
    return remember(launcher) { { launcher.launch(arrayOf("application/pdf")) } }
}
