package io.github.fvory.psdk.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §15.1 R2. */
class RenderSizeTest {
    @Test
    fun normalSizeIsPointsTimesScaleTimesDensity() {
        assertEquals(PixelSize(1785, 2526), RenderSize.compute(595.0, 842.0, 1f, 3f))
    }

    @Test
    fun longestSideIsCappedKeepingAspectRatio() {
        val size = RenderSize.compute(595.0, 842.0, 4f, 3f)
        assertEquals(RenderSize.MAX_SIDE, size.height)
        assertEquals(595.0 / 842.0, size.width.toDouble() / size.height, 0.001)
    }

    @Test
    fun pixelCountIsCapped() {
        // Square page: side cap allows 4096², pixel cap is lower
        val size = RenderSize.compute(1000.0, 1000.0, 4f, 3f)
        assertTrue(size.width.toLong() * size.height <= RenderSize.MAX_PIXELS)
        assertEquals(size.width, size.height)
    }

    @Test
    fun neverZero() {
        val size = RenderSize.compute(1.0, 0.1, 0.25f, 1f)
        assertEquals(1, size.height)
    }
}
