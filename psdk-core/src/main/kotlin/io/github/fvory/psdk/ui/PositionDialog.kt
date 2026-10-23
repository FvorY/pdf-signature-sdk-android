package io.github.fvory.psdk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PsdkLabels

/**
 * Built-in position dialog for [io.github.fvory.psdk.config.PsdkConfig.askPositionOnAdd]. Both
 * fields start empty. Both empty places the item at the default position or the page center; both
 * filled places it at X/Y (kept inside the page); one filled or a non-number disables "Place".
 */
@Composable
internal fun PositionDialog(
    viewer: PdfSignatureViewer,
    prompt: PdfSignatureViewer.PositionPrompt,
    labels: PsdkLabels,
) {
    val colors = LocalPsdkColors.current
    var xText by remember(prompt) { mutableStateOf("") }
    var yText by remember(prompt) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(prompt) { focus.requestFocus() }

    val x = parseCoordinate(xText)
    val y = parseCoordinate(yText)
    val bothEmpty = xText.isBlank() && yText.isBlank()
    val valid = bothEmpty || (x != null && y != null)
    val showError = !valid

    fun submit() {
        if (valid) viewer.confirmPosition(x.takeUnless { bothEmpty }, y.takeUnless { bothEmpty })
    }

    AlertDialog(
        onDismissRequest = viewer::cancelPositionPrompt,
        title = { Text(labels.positionModalTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(labels.positionModalDesc, color = colors.textSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xText,
                        onValueChange = { xText = it },
                        singleLine = true,
                        label = { Text(labels.positionX) },
                        isError = showError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                    OutlinedTextField(
                        value = yText,
                        onValueChange = { yText = it },
                        singleLine = true,
                        label = { Text(labels.positionY) },
                        isError = showError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    if (showError) {
                        labels.positionInvalid
                    } else {
                        labels.positionPageSize.format(formatPoints(prompt.pageWidth), formatPoints(prompt.pageHeight))
                    },
                    color = if (showError) colors.danger else colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = ::submit, enabled = valid) { Text(labels.placeBtn) } },
        dismissButton = { TextButton(onClick = viewer::cancelPositionPrompt) { Text(labels.cancelBtn) } },
    )
}

/** A non-negative finite number, `,` accepted as the decimal separator; null for blank or invalid text. */
internal fun parseCoordinate(text: String): Double? =
    text
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0 }

private fun formatPoints(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
