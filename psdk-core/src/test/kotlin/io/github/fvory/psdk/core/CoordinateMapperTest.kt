package io.github.fvory.psdk.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** TC-U16 — spec §5.3 / §5.4. */
class CoordinateMapperTest {
    @Test
    fun specExampleDragOf30dpAtScale15MovesTwentyPoints() {
        val startX = 217.5
        val screen = CoordinateMapper.pointToScreen(startX, 1.5f)
        assertEquals(326.25f, screen, 0.0001f)
        assertEquals(237.5, CoordinateMapper.screenToPoint(screen + 30f, 1.5f), 1e-6)
    }

    @Test
    fun roundTripAtEveryZoomStep() {
        var scale = ZoomMath.MIN_SCALE
        while (scale <= ZoomMath.MAX_SCALE) {
            for (point in listOf(0.0, 12.34, 297.5, 595.276, 841.89)) {
                val screen = CoordinateMapper.pointToScreen(point, scale, origin = 16f)
                assertEquals(point, CoordinateMapper.screenToPoint(screen, scale, origin = 16f), 0.001)
            }
            scale += ZoomMath.STEP
        }
    }
}
