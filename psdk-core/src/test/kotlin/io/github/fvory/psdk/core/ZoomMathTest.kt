package io.github.fvory.psdk.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** TC-U14, TC-U15 — B-ZM-01, B-ZM-03, B-ZM-04. */
class ZoomMathTest {
    @Test
    fun clampKeepsScaleInRange() {
        assertEquals(0.25f, ZoomMath.clamp(0.1f))
        assertEquals(4f, ZoomMath.clamp(9f))
        assertEquals(1.3f, ZoomMath.clamp(1.3f))
    }

    @Test
    fun stepsByQuarterAndRoundsLikeWeb() {
        assertEquals(1.75f, ZoomMath.stepUp(1.5f))
        assertEquals(1.25f, ZoomMath.stepDown(1.5f))
        // 1.33 + 0.25 = 1.58 (toFixed(2) in the web SDK)
        assertEquals(1.58f, ZoomMath.stepUp(1.33f))
    }

    @Test
    fun stepsStopAtLimits() {
        assertEquals(4f, ZoomMath.stepUp(4f))
        assertEquals(4f, ZoomMath.stepUp(3.9f))
        assertEquals(0.25f, ZoomMath.stepDown(0.25f))
        assertEquals(0.25f, ZoomMath.stepDown(0.3f))
    }

    @Test
    fun fitToScreenUsesSmallerRatioMinus48() {
        // A4 in a 400 × 900 viewport: width ratio (400-48)/595 ≈ 0.5916 is the smaller one
        assertEquals(352f / 595f, ZoomMath.fitToScreen(400f, 900f, 595.0, 842.0), 0.0001f)
        // Landscape viewport: height decides
        assertEquals(552f / 842f, ZoomMath.fitToScreen(1200f, 600f, 595.0, 842.0), 0.0001f)
    }

    @Test
    fun fitToScreenIsClamped() {
        assertEquals(4f, ZoomMath.fitToScreen(10_000f, 10_000f, 100.0, 100.0))
        assertEquals(0.25f, ZoomMath.fitToScreen(60f, 60f, 595.0, 842.0))
    }

    @Test
    fun fitToWidthSubtractsBothPaddings() {
        assertEquals((360f - 32f) / 595f, ZoomMath.fitToWidth(360f, 595.0, 16f), 0.0001f)
    }

    @Test
    fun percentLabelRounds() {
        assertEquals("150%", ZoomMath.percentLabel(1.5f))
        assertEquals("59%", ZoomMath.percentLabel(0.5916f))
    }
}
