package io.github.fvory.psdk.sample

import android.content.Context

/** The release sample does not include psdk-pdfium. */
internal object PdfiumCheck {
    suspend fun run(context: Context): List<String> = listOf("Pdfium is only included in the debug build")
}
