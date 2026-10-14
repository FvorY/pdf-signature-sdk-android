package io.github.fvory.psdk.engine

import io.github.fvory.psdk.error.PsdkException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfHeaderTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun acceptsHeaderAtStart() {
        val file = folder.newFile().apply { writeText("%PDF-1.7\n%âãÏÓ\n") }
        assertTrue(PdfHeader.isPdf(file))
    }

    @Test
    fun acceptsHeaderAfterLeadingJunk() {
        val file = folder.newFile().apply { writeBytes(ByteArray(300) { 0x20 } + "%PDF-1.4".toByteArray()) }
        assertTrue(PdfHeader.isPdf(file))
    }

    @Test
    fun rejectsOtherFiles() {
        assertFalse(PdfHeader.isPdf(folder.newFile().apply { writeText("<html></html>") }))
        assertFalse(PdfHeader.isPdf(folder.newFile()))
        assertFalse(PdfHeader.isPdf(folder.newFile().apply { writeBytes(ByteArray(2000) + "%PDF-1.4".toByteArray()) }))
    }

    @Test(expected = PsdkException.NotAPdf::class)
    fun requirePdfThrows() {
        PdfHeader.requirePdf(folder.newFile().apply { writeText("PK zip") })
    }
}
