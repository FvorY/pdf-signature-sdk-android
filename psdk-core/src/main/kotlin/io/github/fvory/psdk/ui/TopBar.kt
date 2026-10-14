package io.github.fvory.psdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.PsdkState
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.core.ZoomMath
import kotlinx.coroutines.launch

/** Logo, brand text and the 🔒 badge for encrypted PDFs (B-TB-02…04). */
@Composable
internal fun Brand(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    val colors = LocalPsdkColors.current
    val topbar = config.ui.topbar
    Row(verticalAlignment = Alignment.CenterVertically) {
        topbar.logo?.let {
            PsdkImageView(viewer, it, Modifier.heightIn(max = 28.dp).widthIn(max = 120.dp).padding(end = 8.dp))
        }
        ChromeRules.brandText(topbar, config.labels)?.let {
            Text(it, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (state.isPasswordProtected) {
            val description = config.labels.securedBadge
            Icon(
                PsdkIcons.lock,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.padding(start = 8.dp).size(16.dp).semantics { contentDescription = description },
            )
        }
    }
}

/** Phone top bar: brand, custom content and the ⋮ menu with zoom and theme (spec §13.2). */
@Composable
internal fun PhoneTopBar(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
) {
    val colors = LocalPsdkColors.current
    val topbar = config.ui.topbar
    val style = topbar.style
    val content = style?.content ?: colors.textPrimary
    Row(
        Modifier
            .fillMaxWidth()
            .height(style?.height ?: 56.dp)
            .background(style?.background ?: colors.surface)
            .padding(horizontal = style?.horizontalPadding ?: 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { Brand(viewer, state, config, content) }
        topbar.customComponent?.invoke()
        OverflowMenu(viewer, state, config)
    }
    HorizontalDivider(color = colors.border)
}

@Composable
private fun OverflowMenu(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
) {
    val topbar = config.ui.topbar
    if (!topbar.zoom && !topbar.themeToggle) return
    val colors = LocalPsdkColors.current
    val labels = config.labels
    val dark = LocalPsdkDark.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    val enabled = !config.disabled
    Box {
        ToolbarButton(PsdkIcons.more, labels.moreMenu, onClick = { open = true }, enabled = enabled)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (topbar.zoom && state.isLoaded) {
                DropdownMenuItem(
                    text = { Text("${labels.zoomOut} (${ChromeRules.zoomPercent(state.scale)})") },
                    leadingIcon = { Icon(PsdkIcons.zoomOut, null, tint = colors.textSecondary) },
                    onClick = { scope.launch { viewer.setScale(ZoomMath.stepDown(state.scale)) } },
                    enabled = state.scale > ZoomMath.MIN_SCALE,
                )
                DropdownMenuItem(
                    text = { Text(labels.zoomIn) },
                    leadingIcon = { Icon(PsdkIcons.zoomIn, null, tint = colors.textSecondary) },
                    onClick = { scope.launch { viewer.setScale(ZoomMath.stepUp(state.scale)) } },
                    enabled = state.scale < ZoomMath.MAX_SCALE,
                )
                DropdownMenuItem(
                    text = { Text(labels.fitToScreen) },
                    leadingIcon = { Icon(PsdkIcons.fit, null, tint = colors.textSecondary) },
                    onClick = {
                        open = false
                        scope.launch { viewer.fitToScreen() }
                    },
                )
            }
            if (topbar.themeToggle) {
                DropdownMenuItem(
                    text = { Text(labels.toggleTheme) },
                    leadingIcon = { Icon(if (dark) PsdkIcons.sun else PsdkIcons.moon, null, tint = colors.textSecondary) },
                    onClick = {
                        open = false
                        viewer.toggleTheme(dark)
                    },
                )
            }
        }
    }
}

/**
 * Tablet top bar in the web SDK order (B-TB-01): brand · upload │ signature, E-Materai, clear ⟷
 * custom │ theme │ zoom │ pagination.
 */
@Composable
internal fun TabletTopBar(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    onUpload: (() -> Unit)?,
) {
    val colors = LocalPsdkColors.current
    val topbar = config.ui.topbar
    val labels = config.labels
    val style = topbar.style
    val content = style?.content ?: colors.textPrimary
    val enabled = !config.disabled
    val dark = LocalPsdkDark.current
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .height(style?.height ?: 56.dp)
            .background(style?.background ?: colors.surface)
            .padding(horizontal = style?.horizontalPadding ?: 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Brand(viewer, state, config, content)
        Spacer(Modifier.width(12.dp))
        if (topbar.upload && onUpload != null) {
            ToolbarButton(PsdkIcons.upload, labels.uploadBtn, onUpload, label = labels.uploadBtn, enabled = enabled)
        }
        ItemButtons(viewer, state, config, iconOnly = false)
        Spacer(Modifier.weight(1f))
        topbar.customComponent?.invoke()
        if (topbar.themeToggle) {
            ToolbarButton(if (dark) PsdkIcons.sun else PsdkIcons.moon, labels.toggleTheme, { viewer.toggleTheme(dark) }, enabled = enabled)
        }
        if (topbar.zoom && state.isLoaded) {
            ToolbarButton(
                PsdkIcons.zoomOut,
                labels.zoomOut,
                { scope.launch { viewer.setScale(ZoomMath.stepDown(state.scale)) } },
                enabled = enabled && state.scale > ZoomMath.MIN_SCALE,
            )
            Text(
                ChromeRules.zoomPercent(state.scale),
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.widthIn(min = 40.dp),
            )
            ToolbarButton(
                PsdkIcons.zoomIn,
                labels.zoomIn,
                { scope.launch { viewer.setScale(ZoomMath.stepUp(state.scale)) } },
                enabled = enabled && state.scale < ZoomMath.MAX_SCALE,
            )
            ToolbarButton(PsdkIcons.fit, labels.fitToScreen, { scope.launch { viewer.fitToScreen() } }, enabled = enabled)
        }
        if (topbar.pagination && state.isLoaded) PageControls(viewer, state, config, inlineInput = true)
    }
    HorizontalDivider(color = colors.border)
}

/** Signature, E-Materai and "Clear All" buttons, shared by the phone bottom bar and tablet top bar. */
@Composable
internal fun ItemButtons(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    iconOnly: Boolean,
) {
    val topbar = config.ui.topbar
    val labels = config.labels
    val enabled = !config.disabled && state.isLoaded
    if (topbar.signature) {
        ToolbarButton(
            PsdkIcons.signature,
            labels.signatureBtn,
            viewer::openSignatureModal,
            label = labels.signatureBtn.takeUnless { iconOnly },
            kind = ButtonKind.ACCENT,
            enabled = enabled,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
    if (topbar.eStamp) {
        ToolbarButton(
            PsdkIcons.estamp,
            labels.estampBtn,
            viewer::openEStampModal,
            label = labels.estampBtn.takeUnless { iconOnly },
            kind = ButtonKind.DANGER,
            enabled = enabled,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
    // B-TB-05: only while there is something to clear
    val count = state.signatures.size
    if (count > 0) {
        ToolbarButton(
            PsdkIcons.clear,
            labels.clearBtn,
            viewer::clearSignatures,
            label = if (iconOnly) "$count" else "${labels.clearBtn} ($count)",
            enabled = !config.disabled,
        )
    }
}
