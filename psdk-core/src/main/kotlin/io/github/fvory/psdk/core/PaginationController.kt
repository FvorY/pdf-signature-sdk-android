package io.github.fvory.psdk.core

import io.github.fvory.psdk.event.PsdkEvent.PageChanged

/**
 * Page navigation state, ported from the web SDK pagination controller (B-PG-01…07).
 * [onChanged] is called only when the page actually changes.
 */
internal class PaginationController(
    private val onChanged: (page: Int, total: Int, source: PageChanged.Source) -> Unit,
) {
    var currentPage: Int = 1
        private set
    var totalPages: Int = 0
        private set
    var isLocked: Boolean = false
        private set

    val canGoNext: Boolean get() = currentPage < totalPages
    val canGoPrev: Boolean get() = currentPage > 1

    fun setLocked(locked: Boolean) {
        isLocked = locked
    }

    /** Sets the page count and resets to page 1 without an event (B-LOAD-07). */
    fun setTotal(total: Int) {
        totalPages = total
        currentPage = 1
    }

    fun goToPage(
        page: Int,
        source: PageChanged.Source = PageChanged.Source.API,
    ): Boolean {
        if (isLocked || page < 1 || page > totalPages || page == currentPage) return false
        currentPage = page
        onChanged(page, totalPages, source)
        return true
    }

    fun nextPage(): Boolean = goToPage(currentPage + 1)

    fun prevPage(): Boolean = goToPage(currentPage - 1)

    fun reset() {
        currentPage = 1
        totalPages = 0
    }
}
