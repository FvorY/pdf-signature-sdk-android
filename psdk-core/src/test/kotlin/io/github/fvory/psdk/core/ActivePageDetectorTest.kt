package io.github.fvory.psdk.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** B-PG-04: the page with the tallest visible part becomes active. */
class ActivePageDetectorTest {
    @Test
    fun picksTallestVisiblePart() {
        val visible =
            listOf(
                VisiblePage(page = 2, top = -700, bottom = 300),
                VisiblePage(page = 3, top = 312, bottom = 1312),
            )
        // Viewport 0..1000: page 2 shows 300 px, page 3 shows 688 px
        assertEquals(3, ActivePageDetector.pick(visible, 0, 1000, current = 2))
    }

    @Test
    fun firstWinsOnTie() {
        val visible = listOf(VisiblePage(4, -500, 500), VisiblePage(5, 500, 1500))
        assertEquals(4, ActivePageDetector.pick(visible, 0, 1000, current = 1))
    }

    @Test
    fun keepsCurrentWhenNothingVisible() {
        assertEquals(7, ActivePageDetector.pick(emptyList(), 0, 1000, current = 7))
    }
}
