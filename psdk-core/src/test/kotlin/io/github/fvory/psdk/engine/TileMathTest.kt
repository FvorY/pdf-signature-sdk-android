package io.github.fvory.psdk.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileMathTest {
    @Test
    fun a4AtMaxZoomOnA440dpiPhoneNeedsDetail() {
        val full = TileMath.fullSize(595.28, 841.89, 4f, 2.75f)
        val capped = RenderSize.compute(595.28, 841.89, 4f, 2.75f)
        assertEquals(6548, full.width)
        assertTrue(capped.width.toLong() * capped.height <= RenderSize.MAX_PIXELS)
        assertTrue(TileMath.needsDetail(full, capped))
    }

    @Test
    fun normalZoomDoesNotNeedDetail() {
        val full = TileMath.fullSize(595.28, 841.89, 0.6f, 2.75f)
        assertFalse(TileMath.needsDetail(full, RenderSize.compute(595.28, 841.89, 0.6f, 2.75f)))
    }

    @Test
    fun visibleRegionIsIntersectionInPagePixels() {
        val full = PixelSize(6000, 8000)
        // Page scrolled so its top-left is at (-1000, -2500); viewport 0..1080 × 0..2000
        assertEquals(
            PixelRect(1000, 2500, 2080, 4500),
            TileMath.visibleRegion(full, -1000f, -2500f, 0f, 0f, 1080f, 2000f),
        )
    }

    @Test
    fun visibleRegionIsClampedToThePage() {
        val full = PixelSize(500, 700)
        assertEquals(PixelRect(0, 0, 500, 300), TileMath.visibleRegion(full, 100f, 1700f, 0f, 0f, 1080f, 2000f))
    }

    @Test
    fun pageOutsideViewportHasNoRegion() {
        assertNull(TileMath.visibleRegion(PixelSize(500, 700), 0f, 2100f, 0f, 0f, 1080f, 2000f))
        assertNull(TileMath.visibleRegion(PixelSize(500, 700), 0f, -800f, 0f, 0f, 1080f, 2000f))
    }
}
