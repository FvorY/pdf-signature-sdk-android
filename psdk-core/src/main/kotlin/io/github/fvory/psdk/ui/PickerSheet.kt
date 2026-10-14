package io.github.fvory.psdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PickerOption
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.model.ItemType

private const val COLUMNS = 3

/**
 * Signature / E-Materai picker (B-MOD-02…07): a bottom sheet on phones, a dialog on tablets.
 * Closes with the ✕ button, back, swipe down or a tap outside.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PickerSheet(
    viewer: PdfSignatureViewer,
    type: ItemType,
    config: PsdkConfig,
    tablet: Boolean,
) {
    val colors = LocalPsdkColors.current
    if (tablet) {
        Dialog(onDismissRequest = viewer::closePicker) {
            Column(Modifier.width(480.dp).clip(RoundedCornerShape(14.dp)).background(colors.surface)) {
                PickerContent(viewer, type, config)
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = viewer::closePicker,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            Column(Modifier.navigationBarsPadding()) { PickerContent(viewer, type, config) }
        }
    }
}

@Composable
private fun PickerContent(
    viewer: PdfSignatureViewer,
    type: ItemType,
    config: PsdkConfig,
) {
    val colors = LocalPsdkColors.current
    val labels = config.labels
    val isSignature = type == ItemType.SIGNATURE
    val options = if (isSignature) config.signatureOptions else config.estampOptions
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isSignature) labels.signatureModalTitle else labels.estampModalTitle,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(PsdkIcons.close, labels.cancelBtn, viewer::closePicker)
    }
    Column(
        Modifier
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (config.groupByCategory) {
            for ((group, items) in ChromeRules.group(options, labels.defaultGroup)) {
                Text(
                    group.uppercase(),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp),
                )
                OptionGrid(viewer, type, items, config)
            }
        } else {
            OptionGrid(viewer, type, options, config)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OptionGrid(
    viewer: PdfSignatureViewer,
    type: ItemType,
    options: List<PickerOption>,
    config: PsdkConfig,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in options.chunked(COLUMNS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in row) OptionTile(viewer, type, option, config, Modifier.weight(1f))
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun OptionTile(
    viewer: PdfSignatureViewer,
    type: ItemType,
    option: PickerOption,
    config: PsdkConfig,
    modifier: Modifier,
) {
    val colors = LocalPsdkColors.current
    val isSignature = type == ItemType.SIGNATURE
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .clickable(role = Role.Button) { viewer.choose(type, option) }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            val image = option.image
            if (image != null) {
                PsdkImageView(viewer, image, Modifier.fillMaxWidth().height(56.dp))
            } else {
                Icon(
                    if (isSignature) PsdkIcons.user else PsdkIcons.estamp,
                    contentDescription = null,
                    tint = if (isSignature) colors.accent else colors.danger,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            ChromeRules.optionLabel(option, isSignature, config.labels),
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
