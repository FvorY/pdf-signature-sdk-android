package io.github.fvory.psdk.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import io.github.fvory.psdk.config.PsdkColors
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.config.PsdkThemeMode

internal val LocalPsdkColors = staticCompositionLocalOf { PsdkColors.light() }
internal val LocalPsdkDark = staticCompositionLocalOf { false }

/** Resolved light/dark choice for [config]. */
@Composable
internal fun isDark(config: PsdkConfig): Boolean =
    when (config.theme) {
        PsdkThemeMode.LIGHT -> false
        PsdkThemeMode.DARK -> true
        PsdkThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

/**
 * Provides the SDK colors and maps them onto a Material 3 scheme, so Material components used
 * inside (sheet, dialog, text field) follow the SDK theme instead of the host app's theme.
 */
@Composable
internal fun PsdkTheme(
    config: PsdkConfig,
    content: @Composable () -> Unit,
) {
    val dark = isDark(config)
    val colors = if (dark) config.darkColors else config.lightColors
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme =
        base.copy(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.accent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.surface,
            surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.surfaceElevated,
            outline = colors.borderStrong,
            outlineVariant = colors.border,
            error = colors.danger,
        )
    val typography =
        config.fontFamily?.let { font ->
            val t = MaterialTheme.typography

            fun TextStyle.withFont() = copy(fontFamily = font)
            t.copy(
                titleMedium = t.titleMedium.withFont(),
                titleSmall = t.titleSmall.withFont(),
                bodyMedium = t.bodyMedium.withFont(),
                bodySmall = t.bodySmall.withFont(),
                labelLarge = t.labelLarge.withFont(),
                labelMedium = t.labelMedium.withFont(),
                labelSmall = t.labelSmall.withFont(),
            )
        } ?: MaterialTheme.typography
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalPsdkColors provides colors, LocalPsdkDark provides dark, content = content)
    }
}
