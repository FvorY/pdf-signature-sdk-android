package io.github.fvory.psdk.event

import io.github.fvory.psdk.model.SignatureItem

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

    /** A signature was placed, from the picker or `placeSignature` (web: `signaturePlaced`). */
    public data class SignaturePlaced(
        public val item: SignatureItem,
    ) : PsdkEvent

    /** An E-Materai was placed (web: `eStampPlaced`). */
    public data class EStampPlaced(
        public val item: SignatureItem,
    ) : PsdkEvent

    /**
     * An item was moved or resized and the finger was lifted, or its image loaded and set its
     * height (web: `signatureMoved`). Sent for signatures and E-Materai.
     */
    public data class SignatureMoved(
        public val item: SignatureItem,
    ) : PsdkEvent

    /** An item was deleted with its ✕ button or `removeSignature` (web: `signatureRemoved`). */
    public data class SignatureRemoved(
        public val id: String,
    ) : PsdkEvent

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
