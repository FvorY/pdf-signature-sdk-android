package io.github.fvory.psdk.core

import io.github.fvory.psdk.event.PsdkEvent.PageChanged.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-U13 — B-PG-01, B-PG-03, B-LOAD-07. */
class PaginationControllerTest {
    private val changes = mutableListOf<Triple<Int, Int, Source>>()
    private val pagination = PaginationController { page, total, source -> changes += Triple(page, total, source) }

    @Test
    fun setTotalResetsToFirstPageWithoutEvent() {
        pagination.setTotal(5)
        pagination.goToPage(3)
        changes.clear()

        pagination.setTotal(8)

        assertEquals(1, pagination.currentPage)
        assertEquals(8, pagination.totalPages)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun goToPageEmitsOnceWithSource() {
        pagination.setTotal(5)

        assertTrue(pagination.goToPage(4, Source.SCROLL))

        assertEquals(listOf(Triple(4, 5, Source.SCROLL)), changes)
        assertEquals(4, pagination.currentPage)
    }

    @Test
    fun goToPageRejectsOutOfRangeSamePageAndLocked() {
        pagination.setTotal(5)

        assertFalse(pagination.goToPage(0))
        assertFalse(pagination.goToPage(6))
        assertFalse(pagination.goToPage(1))

        pagination.setLocked(true)
        assertFalse(pagination.goToPage(2))
        assertFalse(pagination.goToPage(2, Source.SCROLL))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun nextAndPrevStopAtEdges() {
        pagination.setTotal(2)

        assertFalse(pagination.prevPage())
        assertTrue(pagination.nextPage())
        assertFalse(pagination.nextPage())
        assertTrue(pagination.prevPage())
        assertEquals(1, pagination.currentPage)
        assertEquals(listOf(2, 1), changes.map { it.first })
    }

    @Test
    fun canGoFlags() {
        pagination.setTotal(3)
        assertFalse(pagination.canGoPrev)
        assertTrue(pagination.canGoNext)
        pagination.goToPage(3)
        assertTrue(pagination.canGoPrev)
        assertFalse(pagination.canGoNext)
    }

    @Test
    fun resetClearsTotal() {
        pagination.setTotal(3)
        pagination.goToPage(2)
        pagination.reset()
        assertEquals(1, pagination.currentPage)
        assertEquals(0, pagination.totalPages)
    }
}
