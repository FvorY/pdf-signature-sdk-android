package io.github.fvory.psdk.config

import androidx.compose.ui.graphics.Color

/** Light, dark, or follow the phone's dark mode. */
public enum class PsdkThemeMode {
    LIGHT,
    DARK,

    /** Mobile addition: follows the system dark mode and changes with it. */
    SYSTEM,
}

/**
 * Color tokens, mapped from the web SDK's `--psdk-*` CSS variables (spec §12).
 * Start from [light] or [dark] and change what you need with `copy(...)`.
 * PDF pages are always drawn on white.
 */
public data class PsdkColors(
    /** `--psdk-bg-base`: behind the pages. */
    public val background: Color,
    /** `--psdk-bg-surface`: toolbars, sheets, sidebar. */
    public val surface: Color,
    /** `--psdk-bg-elevated`: badges, inputs. */
    public val surfaceElevated: Color,
    /** `--psdk-bg-hover`: pressed buttons. */
    public val surfacePressed: Color,
    public val border: Color,
    public val borderStrong: Color,
    /** `--psdk-accent`: primary buttons, signature placeholders, active thumbnail. */
    public val accent: Color,
    /** `--psdk-accent-hover`. */
    public val accentPressed: Color,
    public val accentSubtle: Color,
    /** Text and icons on [accent]. */
    public val onAccent: Color,
    /** `--psdk-danger`: delete buttons, E-Materai placeholders. */
    public val danger: Color,
    public val dangerSubtle: Color,
    public val success: Color,
    public val warning: Color,
    public val textPrimary: Color,
    public val textSecondary: Color,
    public val textMuted: Color,
) {
    public companion object {
        public fun light(): PsdkColors =
            PsdkColors(
                background = Color(0xFFF8FAFC),
                surface = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFF1F5F9),
                surfacePressed = Color(0xFFE2E8F0),
                border = Color(0x14000000),
                borderStrong = Color(0x24000000),
                accent = Color(0xFF14A1C5),
                accentPressed = Color(0xFF108BA8),
                accentSubtle = Color(0x1F14A1C5),
                onAccent = Color(0xFFFFFFFF),
                danger = Color(0xFFEF4444),
                dangerSubtle = Color(0x1FEF4444),
                success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569),
                textMuted = Color(0xFF64748B),
            )

        public fun dark(): PsdkColors =
            light().copy(
                background = Color(0xFF0F1117),
                surface = Color(0xFF1A1D27),
                surfaceElevated = Color(0xFF22263A),
                surfacePressed = Color(0xFF2A2F45),
                border = Color(0x14FFFFFF),
                borderStrong = Color(0x24FFFFFF),
                textPrimary = Color(0xFFF1F5F9),
                textSecondary = Color(0xFF8B92A5),
                textMuted = Color(0xFF5A6070),
            )
    }
}
