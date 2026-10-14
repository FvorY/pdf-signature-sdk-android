package io.github.fvory.psdk.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** TC-U11, TC-U12 — B-INT-04…06. */
class ItemGeometryTest {
    private val box = ItemBox(100.0, 200.0, 160.0, 48.0)

    @Test
    fun dragKeepsGrabOffset() {
        // Grabbed 10 pt right and 5 pt below the corner, pointer now at (300, 400)
        val moved = ItemGeometry.drag(box, 300.0, 400.0, 10.0, 5.0, 595.0, 842.0)
        assertEquals(290.0, moved.x, 0.0)
        assertEquals(395.0, moved.y, 0.0)
        assertEquals(160.0, moved.width, 0.0)
    }

    @Test
    fun dragIsClampedToAllFourEdges() {
        assertEquals(0.0, ItemGeometry.drag(box, -50.0, 0.0, 0.0, 0.0, 595.0, 842.0).x, 0.0)
        assertEquals(0.0, ItemGeometry.drag(box, 0.0, -50.0, 0.0, 0.0, 595.0, 842.0).y, 0.0)
        assertEquals(595.0 - 160.0, ItemGeometry.drag(box, 1000.0, 0.0, 0.0, 0.0, 595.0, 842.0).x, 0.0)
        assertEquals(842.0 - 48.0, ItemGeometry.drag(box, 0.0, 1000.0, 0.0, 0.0, 595.0, 842.0).y, 0.0)
    }

    @Test
    fun resizeChangesWidthAndHeightIndependently() {
        val resized = ItemGeometry.resize(box, 40.0, -8.0, 595.0, 842.0)
        assertEquals(200.0, resized.width, 0.0)
        assertEquals(40.0, resized.height, 0.0)
        assertEquals(100.0, resized.x, 0.0)
    }

    @Test
    fun resizeHasMinimumOf20Points() {
        val resized = ItemGeometry.resize(box, -500.0, -500.0, 595.0, 842.0)
        assertEquals(20.0, resized.width, 0.0)
        assertEquals(20.0, resized.height, 0.0)
    }

    @Test
    fun resizeStopsAtPageEdge() {
        val resized = ItemGeometry.resize(box, 1000.0, 1000.0, 595.0, 842.0)
        assertEquals(495.0, resized.width, 0.0)
        assertEquals(642.0, resized.height, 0.0)
    }

    @Test
    fun resizeKeepingAspectRatio() {
        val stamp = ItemBox(100.0, 100.0, 80.0, 80.0)
        val resized = ItemGeometry.resize(stamp, 20.0, -50.0, 595.0, 842.0, keepAspectRatio = true)
        assertEquals(100.0, resized.width, 1e-9)
        assertEquals(100.0, resized.height, 1e-9)
        // Height limit wins near the bottom of the page
        val nearBottom = ItemBox(100.0, 800.0, 30.0, 30.0)
        val limited = ItemGeometry.resize(nearBottom, 100.0, 0.0, 595.0, 842.0, keepAspectRatio = true)
        assertEquals(42.0, limited.height, 1e-9)
        assertEquals(42.0, limited.width, 1e-9)
    }
}
