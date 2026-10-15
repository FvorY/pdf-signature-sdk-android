package io.github.fvory.psdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.PsdkState
import io.github.fvory.psdk.config.PsdkConfig

/**
 * Prev · "current / total" · next (B-PG-07/08). With [inlineInput] the current page is an editable
 * field (tablet); otherwise tapping it opens [GoToPageDialog] (phone).
 */
@Composable
internal fun PageControls(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    inlineInput: Boolean,
    onOpenDialog: () -> Unit = {},
) {
    val colors = LocalPsdkColors.current
    val labels = config.labels
    val locked = state.isPaginationLocked
    val usable = !config.disabled && !locked
    val inputEnabled = usable && config.ui.topbar.paginationInput
    Row(verticalAlignment = Alignment.CenterVertically) {
        ToolbarButton(PsdkIcons.prevPage, labels.prevPage, { viewer.prevPage() }, enabled = usable && state.currentPage > 1)
        if (inlineInput) {
            var text by remember(state.currentPage) { mutableStateOf(state.currentPage.toString()) }
            BasicTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(5) },
                enabled = inputEnabled,
                singleLine = true,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions =
                    KeyboardActions(onGo = {
                        val page = ChromeRules.parsePage(text)
                        if (page == null || !viewer.goToPage(page)) text = viewer.currentPage.toString()
                    }),
                modifier =
                    Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                        .padding(vertical = 6.dp)
                        .semantics { contentDescription = labels.goToPageTitle },
            )
            Text(" / ${state.totalPages}", color = colors.textSecondary, fontSize = 13.sp)
        } else {
            val description = labels.a11yPage.format(state.currentPage, state.totalPages)
            Text(
                "${state.currentPage} / ${state.totalPages}",
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = inputEnabled, role = Role.Button, onClick = onOpenDialog)
                        .semantics { contentDescription = description }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        ToolbarButton(PsdkIcons.nextPage, labels.nextPage, { viewer.nextPage() }, enabled = usable && state.currentPage < state.totalPages)
    }
}

/** Floating page indicator at the bottom of the pages on phones (spec §13.2). */
@Composable
internal fun PaginationPill(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsdkColors.current
    Row(
        modifier
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .graphicsLayer { alpha = if (config.disabled) 0.6f else 1f }
            .padding(horizontal = 4.dp),
    ) {
        PageControls(viewer, state, config, inlineInput = false, onOpenDialog = onOpenDialog)
    }
}

/** "Go to page" dialog. A bad number keeps the current page (B-PG-08). */
@Composable
internal fun GoToPageDialog(
    viewer: PdfSignatureViewer,
    state: PsdkState,
    config: PsdkConfig,
    onDismiss: () -> Unit,
) {
    val labels = config.labels
    var text by remember { mutableStateOf(state.currentPage.toString()) }
    LaunchedEffect(Unit) { text = state.currentPage.toString() }

    fun submit() {
        ChromeRules.parsePage(text)?.let { viewer.goToPage(it) }
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.goToPageTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(5) },
                singleLine = true,
                suffix = { Text("/ ${state.totalPages}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
            )
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(labels.okBtn) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(labels.cancelBtn) } },
    )
}
