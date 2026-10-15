package io.github.fvory.psdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PsdkLabels

/**
 * Built-in password dialog (B-PWD-01…06). An empty field does nothing; while unlocking every
 * control is disabled; a wrong password shows the error with an empty, focused field; Cancel,
 * back or a tap outside cancels the load. The password is never logged or stored (B-PWD-07).
 */
@Composable
internal fun PasswordDialog(
    viewer: PdfSignatureViewer,
    prompt: PdfSignatureViewer.PasswordPrompt,
    labels: PsdkLabels,
) {
    val colors = LocalPsdkColors.current
    // A new prompt (e.g. after a wrong password) starts with an empty field
    var password by remember(prompt.isIncorrect, prompt.submitting) { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val busy = prompt.submitting
    LaunchedEffect(prompt) { if (!busy) focus.requestFocus() }

    fun submit() {
        if (password.isNotEmpty() && !busy) viewer.submitPassword(password)
    }

    AlertDialog(
        onDismissRequest = { if (!busy) viewer.cancelPassword() },
        properties = DialogProperties(dismissOnClickOutside = !busy, dismissOnBackPress = !busy),
        icon = {
            Box(Modifier.size(48.dp).clip(CircleShape).background(colors.dangerSubtle), contentAlignment = Alignment.Center) {
                Icon(PsdkIcons.lock, contentDescription = null, tint = colors.danger, modifier = Modifier.size(24.dp))
            }
        },
        title = { Text(labels.passwordModalTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    labels.passwordModalDesc,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    enabled = !busy,
                    singleLine = true,
                    placeholder = { Text(labels.passwordPlaceholder) },
                    isError = prompt.isIncorrect,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            autoCorrectEnabled = false,
                        ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }, enabled = !busy) {
                            Icon(if (visible) PsdkIcons.eyeOff else PsdkIcons.eye, contentDescription = null, tint = colors.textSecondary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (prompt.isIncorrect && !busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(labels.passwordIncorrect, color = colors.danger, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = !busy && password.isNotEmpty()) {
                Text(if (busy) labels.unlockingBtn else labels.unlockBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = viewer::cancelPassword, enabled = !busy) { Text(labels.cancelBtn) }
        },
    )
}
