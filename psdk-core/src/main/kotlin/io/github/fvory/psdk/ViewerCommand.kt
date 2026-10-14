package io.github.fvory.psdk

/** Requests from the viewer to its Compose UI. */
internal sealed interface ViewerCommand {
    /**
     * Scroll so [page] is at the top. Scroll-based page detection stays off while scrolling and for
     * [settleMillis] afterwards (B-PG-05: 150 ms after `goToPage`; B-ZM-01: 850 ms after `setScale`).
     */
    data class ScrollToPage(
        val page: Int,
        val animate: Boolean,
        val settleMillis: Long,
    ) : ViewerCommand
}
