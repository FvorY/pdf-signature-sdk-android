package io.github.fvory.psdk.event

/**
 * Events emitted by the viewer, mirroring the web SDK event names.
 * Listeners are always called on the main thread, in the order the events happen.
 */
public sealed interface PsdkEvent {
    /** A PDF finished loading (web: `documentLoaded`). */
    public data class DocumentLoaded(
        public val totalPages: Int,
    ) : PsdkEvent

    /** The active page changed (web: `pageChanged`). */
    public data class PageChanged(
        public val page: Int,
        public val total: Int,
        public val source: Source,
    ) : PsdkEvent {
        /** What caused the change. */
        public enum class Source {
            /** The user scrolled. */
            SCROLL,

            /** `goToPage`, `nextPage`, `prevPage`, a thumbnail or the page input. */
            API,
        }
    }

    /**
     * All signatures and E-Materai were removed (web: `signaturesCleared`).
     * Also sent right before [DocumentLoaded], because loading a document clears them.
     */
    public data object SignaturesCleared : PsdkEvent
}

/** Receives one type of [PsdkEvent]. */
public fun interface PsdkListener<E : PsdkEvent> {
    public fun onEvent(event: E)
}

/** Returned by `on`/`once`. Call [cancel] to stop listening (web: `off`). */
public fun interface PsdkSubscription {
    public fun cancel()
}
