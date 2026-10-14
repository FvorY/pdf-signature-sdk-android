package io.github.fvory.psdk

import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.SignatureItem

/** Snapshot of the viewer, exposed as `PdfSignatureViewer.state` for Compose and Flow users. */
public data class PsdkState(
    public val isLoading: Boolean = false,
    public val isLoaded: Boolean = false,
    public val currentPage: Int = 1,
    public val totalPages: Int = 0,
    public val scale: Float = 1.5f,
    public val isPaginationLocked: Boolean = false,
    public val isPasswordProtected: Boolean = false,
    /** Page sizes in PDF points, index 0 = page 1. Empty when no document is loaded. */
    public val pageSizes: List<PageSize> = emptyList(),
    /** Placed signatures and E-Materai, same as `getSignatures()`. */
    public val signatures: List<SignatureItem> = emptyList(),
    /** Error of the last failed load, cleared when a new load starts. */
    public val error: Throwable? = null,
    /** Changes every time a document is opened, so the UI can drop state of the previous one. */
    internal val documentId: Long = 0,
)
