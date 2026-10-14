package io.github.fvory.psdk.model

/**
 * Options of `placeSignature` / `placeEStamp`, same as the web SDK.
 *
 * Positions and sizes are in PDF points. When [x] or [y] is missing, the item is centered on the
 * page. When [page] is missing, the current page is used.
 */
public data class PlaceOptions(
    public val x: Double? = null,
    public val y: Double? = null,
    public val page: Int? = null,
    /** Text on a signature without an image. Ignored for E-Materai, which is always labelled "E-Materai". */
    public val label: String? = null,
    public val image: PsdkImage? = null,
    /** Default 160 for signatures, 80 for E-Materai. */
    public val width: Double? = null,
    /** Default 48 for signatures, 80 for E-Materai. Without it, a loaded image sets the height from its aspect ratio. */
    public val height: Double? = null,
    /** Locks moving this item only. */
    public val disableDrag: Boolean = false,
    /** Locks moving and resizing this item only. */
    public val disableDragging: Boolean = false,
    /** Locks resizing this item only. */
    public val disableResize: Boolean = false,
    /** Locks moving and resizing this item only. */
    public val locked: Boolean = false,
)
