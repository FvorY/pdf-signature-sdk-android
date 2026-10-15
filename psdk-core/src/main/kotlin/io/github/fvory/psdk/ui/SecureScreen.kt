package io.github.fvory.psdk.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Sets `FLAG_SECURE` on the hosting Activity window while [enabled] (config `secureScreen`, Q9).
 * Removes it on dispose only when it was not already set, so an app that secures its own window
 * keeps it.
 */
@Composable
internal fun SecureScreen(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity, enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val window = activity.window
        val alreadySecure = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        if (!alreadySecure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!alreadySecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
