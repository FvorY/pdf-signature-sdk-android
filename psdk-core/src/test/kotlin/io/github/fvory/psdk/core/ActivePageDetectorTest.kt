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

    @Test
    fun shortLastPageBecomesActiveAtTheEnd() {
        // Portrait page 4 fills most of the screen, landscape page 5 is shorter, list is at its end
        val visible = listOf(VisiblePage(4, -300, 700), VisiblePage(5, 712, 1000))
        assertEquals(4, ActivePageDetector.pick(visible, 0, 1000, current = 4, canScrollBackward = true, canScrollForward = true))
        assertEquals(5, ActivePageDetector.pick(visible, 0, 1000, current = 4, canScrollBackward = true, canScrollForward = false))
    }

    @Test
    fun allPagesFitOnScreenUsesTallestPart() {
        // Nothing scrolls: do not jump to the last page
        val visible = listOf(VisiblePage(1, 16, 516), VisiblePage(2, 528, 828))
        assertEquals(1, ActivePageDetector.pick(visible, 0, 1000, current = 1, canScrollBackward = false, canScrollForward = false))
    }
}
