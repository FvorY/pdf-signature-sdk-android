package io.github.fvory.psdk.config

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** B-A11Y-04: the default themes pass WCAG AA (4.5:1) for every text color on its background. */
class PsdkColorsContrastTest {
    private fun channel(value: Float): Double = if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double = 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertAa(
        theme: String,
        colors: PsdkColors,
    ) {
        val pairs =
            mapOf(
                "textPrimary/surface" to (colors.textPrimary to colors.surface),
                "textPrimary/background" to (colors.textPrimary to colors.background),
                "textSecondary/surface" to (colors.textSecondary to colors.surface),
                "textSecondary/surfaceElevated" to (colors.textSecondary to colors.surfaceElevated),
                "textMuted/surface" to (colors.textMuted to colors.surface),
                "textMuted/background" to (colors.textMuted to colors.background),
                "accent/surface" to (colors.accent to colors.surface),
                "danger/surface" to (colors.danger to colors.surface),
                "onAccent/accent" to (colors.onAccent to colors.accent),
                "onDanger/danger" to (colors.onDanger to colors.danger),
            )
        for ((name, pair) in pairs) {
            val ratio = contrast(pair.first, pair.second)
            assertTrue("$theme $name is ${"%.2f".format(ratio)}:1, needs 4.5:1", ratio >= 4.5)
        }
    }

    @Test
    fun lightPassesAa() = assertAa("light", PsdkColors.light())

    @Test
    fun darkPassesAa() = assertAa("dark", PsdkColors.dark())
}
