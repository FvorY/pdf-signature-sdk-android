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
 *
 * The defaults are the web SDK colors adjusted to pass WCAG AA (4.5:1) for text (B-A11Y-04):
 * light accent #14A1C5 → #0E7C99 and danger #EF4444 → #DC2626; dark muted text #5A6070 → #848BA0,
 * dark danger → #F26B6B, and dark buttons use dark text. [webLight] and [webDark] give the exact
 * web colors.
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
    /** Text and icons on [danger]. */
    public val onDanger: Color,
    public val dangerSubtle: Color,
    public val success: Color,
    public val warning: Color,
    public val textPrimary: Color,
    public val textSecondary: Color,
    public val textMuted: Color,
) {
    public companion object {
        /** Light theme, WCAG AA. */
        public fun light(): PsdkColors =
            webLight().copy(
                accent = Color(0xFF0E7C99),
                accentPressed = Color(0xFF0B6680),
                accentSubtle = Color(0x1F0E7C99),
                danger = Color(0xFFDC2626),
                dangerSubtle = Color(0x1FDC2626),
            )

        /** Dark theme, WCAG AA. */
        public fun dark(): PsdkColors =
            webDark().copy(
                onAccent = Color(0xFF0F1117),
                accentPressed = Color(0xFF3BB4D3),
                danger = Color(0xFFF26B6B),
                dangerSubtle = Color(0x1FF26B6B),
                onDanger = Color(0xFF0F1117),
                textMuted = Color(0xFF848BA0),
            )

        /** The web SDK's light colors, unchanged. */
        public fun webLight(): PsdkColors =
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
                onDanger = Color(0xFFFFFFFF),
                dangerSubtle = Color(0x1FEF4444),
                success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569),
                textMuted = Color(0xFF64748B),
            )

        /** The web SDK's dark colors, unchanged. */
        public fun webDark(): PsdkColors =
            webLight().copy(
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
