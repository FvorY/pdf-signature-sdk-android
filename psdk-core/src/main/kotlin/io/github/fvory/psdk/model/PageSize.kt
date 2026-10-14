package io.github.fvory.psdk.model

/**
 * Size of a PDF page in PDF points (1/72 inch), after the page's `/Rotate` is applied.
 * An A4 portrait page is about 595 × 842.
 */
public data class PageSize(
    public val width: Double,
    public val height: Double,
)
