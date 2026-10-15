package io.github.fvory.psdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.PsdkState
import io.github.fvory.psdk.config.PsdkConfig

/** Phone bottom bar: upload, signature, E-Materai, clear (spec §13.2). Hidden when it would be empty. */
@Composable
internal fun PhoneBottomBar(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    onUpload: (() -> Unit)?,
    iconOnly: Boolean,
) {
    val topbar = config.ui.topbar
    val showUpload = topbar.upload && onUpload != null
    if (!showUpload && !topbar.signature && !topbar.eStamp && state.signatures.isEmpty()) return
    val colors = LocalPsdkColors.current
    val labels = config.labels
    HorizontalDivider(color = colors.border)
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showUpload) {
            ToolbarButton(
                PsdkIcons.upload,
                labels.uploadBtn,
                onUpload!!,
                label = labels.uploadBtn.takeUnless { iconOnly },
                enabled = !config.disabled,
            )
        }
        ItemButtons(viewer, state, config, iconOnly)
    }
}

/** "● Ready · 3 signatures · 150%" (B-ST-01). */
@Composable
internal fun StatusBar(
    state: PsdkState,
    config: PsdkConfig,
) {
    val colors = LocalPsdkColors.current
    val labels = config.labels
    Row(
        Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(colors.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (state.isLoading) colors.warning else colors.success))
        Text(labels.statusReady, color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
        Spacer(Modifier.weight(1f))
        ChromeRules.itemCount(state.signatures.size, labels)?.let {
            Text(it, color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(end = 12.dp))
        }
        if (state.isLoaded) Text(ChromeRules.zoomPercent(state.scale), color = colors.textMuted, fontSize = 11.sp)
    }
}
