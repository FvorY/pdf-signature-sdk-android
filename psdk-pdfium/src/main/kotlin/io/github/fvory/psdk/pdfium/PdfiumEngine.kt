package io.github.fvory.psdk.pdfium

/**
 * Optional rendering engine based on Pdfium.
 *
 * Android's built-in `PdfRenderer` only opens password-protected PDFs on API 35+.
 * Adding this module enables them on API 24–34. Implemented in milestone M4.
 */
public object PdfiumEngine
