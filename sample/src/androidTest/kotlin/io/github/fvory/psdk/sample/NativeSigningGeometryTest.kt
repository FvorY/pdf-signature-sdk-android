package io.github.fvory.psdk.sample

import android.util.Base64
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.config.PlacementPosition
import io.github.fvory.psdk.config.PsdkConfig
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSigningGeometryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun nativeSmallPageIsNotReadAsA4() =
        runBlocking<Unit> {
            verify(smallPdf(), expectedPages = 1)
        }

    @Test
    fun providedDocumentNativeGeometryAndExportUseOriginalBytes() =
        runBlocking<Unit> {
            assumeTrue(
                "Optional local regression PDF is not included in CI",
                "provided.pdf" in
                    instrumentation.context.assets
                        .list("")
                        .orEmpty(),
            )
            val bytes =
                instrumentation.context.assets
                    .open("provided.pdf")
                    .use { it.readBytes() }
            verify(bytes, expectedPages = 16)
        }

    @Test
    fun defaultPositionPlacesUiAddedItemsWithoutDragging() =
        runBlocking<Unit> {
            withContext(Dispatchers.Main) {
                val viewer =
                    PdfSignatureViewer(
                        instrumentation.targetContext,
                        PsdkConfig(defaultPosition = PlacementPosition(10.0, 20.0)),
                    )
                try {
                    viewer.loadDocument(PdfSource.BytesSource(smallPdf()))
                    viewer.openSignatureModal()
                    viewer.openEStampModal()
                    val (signature, estamp) = viewer.getSignatures().let { it[0] to it[1] }
                    assertEquals(10.0, signature.x, 0.0001)
                    assertEquals(20.0, signature.y, 0.0001)
                    assertEquals(10.0, estamp.x, 0.0001)
                    // x=500 must be clamped inside the page.
                    viewer.updateConfig {
                        it.copy(defaultPosition = PlacementPosition(500.0, 500.0))
                    }
                    viewer.openSignatureModal()
                    val clamped = viewer.getSignatures().last()
                    val page = viewer.getPageSize(1)
                    assertEquals(page.width - 160.0, clamped.x, 0.0001)
                    assertEquals(page.height - 48.0, clamped.y, 0.0001)
                } finally {
                    viewer.destroy()
                }
            }
        }

    @Test
    fun staleA4DimensionsAreRejectedAfterLoadingSmallDocument() =
        runBlocking<Unit> {
            withContext(Dispatchers.Main) {
                val viewer = PdfSignatureViewer(instrumentation.targetContext)
                try {
                    viewer.loadDocument(PdfSource.BytesSource(smallPdf()))
                    val id = viewer.placeSignature(PlaceOptions(x = 0.0, y = 0.0, width = 25.0, height = 46.0))
                    var rejected = false
                    try {
                        viewer.exportInitSignDocument(mapOf(id to "test-specimen"), listOf(PageSize(595.0, 841.0)))
                    } catch (_: IllegalArgumentException) {
                        rejected = true
                    }
                    assertTrue("A4 metadata must not be exported for this small PDF", rejected)
                } finally {
                    viewer.destroy()
                }
            }
        }

    @Test
    fun replacingA4WithSmallPdfDoesNotKeepOldPageSize() =
        runBlocking<Unit> {
            val a4 = smallPdf(595.28, 841.89)
            val small = smallPdf()
            val firstMetadata = withContext(Dispatchers.IO) { CoordinatePdfMetadata.read(a4) }
            assertEquals(595.28, firstMetadata[0].width, 0.0001)
            assertEquals(841.89, firstMetadata[0].height, 0.0001)
            val secondMetadata = withContext(Dispatchers.IO) { CoordinatePdfMetadata.read(small) }
            assertEquals(198.0, secondMetadata[0].width, 0.0001)
            assertEquals(255.12, secondMetadata[0].height, 0.0001)
            withContext(Dispatchers.Main) {
                val viewer = PdfSignatureViewer(instrumentation.targetContext)
                try {
                    viewer.loadDocument(PdfSource.BytesSource(a4))
                    assertTrue(viewer.getPageSize(1).width > 590)
                    viewer.placeSignature(PlaceOptions(x = 0.0, y = 0.0))
                    viewer.loadDocument(PdfSource.BytesSource(small))
                    assertTrue(viewer.getSignatures().isEmpty())
                    assertEquals(198.0, viewer.getPageSize(1).width, 0.0001)
                    val id = viewer.placeSignature(PlaceOptions(x = 0.0, y = 0.0, width = 25.0, height = 46.0))
                    val exported = viewer.exportInitSignDocument(mapOf(id to "test"), secondMetadata)
                    assertArrayEquals(small, Base64.decode(exported.base64Pdf, Base64.DEFAULT))
                } finally {
                    viewer.destroy()
                }
            }
        }

    @Test
    fun providedPdfRendersTopLeftPlacementInRealSdkUi() =
        runBlocking<Unit> {
            assumeTrue(
                "Optional local regression PDF is not included in CI",
                "provided.pdf" in
                    instrumentation.context.assets
                        .list("")
                        .orEmpty(),
            )
            val bytes =
                instrumentation.context.assets
                    .open("provided.pdf")
                    .use { it.readBytes() }
            val scenario =
                androidx.test.core.app.ActivityScenario
                    .launch(NativeGeometryActivity::class.java)
            try {
                lateinit var viewer: PdfSignatureViewer
                scenario.onActivity { viewer = it.viewer }
                withContext(Dispatchers.Main) {
                    viewer.loadDocument(PdfSource.BytesSource(bytes))
                    assertEquals(198.0, viewer.getPageSize(1).width, 0.0001)
                    viewer.placeSignature(
                        PlaceOptions(
                            x = 0.0,
                            y = 0.0,
                            page = 1,
                            width = 25.00148397493763,
                            height = 46.08857727510421,
                            image =
                                io.github.fvory.psdk.model.PsdkImage
                                    .BitmapImage(SampleImages.signature),
                        ),
                    )
                }
                instrumentation.waitForIdleSync()
                Thread.sleep(1500)
                val bitmap = instrumentation.uiAutomation.takeScreenshot()
                val output = java.io.File(instrumentation.targetContext.cacheDir, "sdk-provided-native.png")
                output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                Log.i("CoordinateRegression", "SDK screenshot saved: ${output.name}")
            } finally {
                scenario.close()
            }
        }

    private suspend fun verify(
        bytes: ByteArray,
        expectedPages: Int,
    ) {
        val exact = withContext(Dispatchers.IO) { CoordinatePdfMetadata.read(bytes) }
        assertEquals(expectedPages, exact.size)
        exact.forEach { size ->
            assertEquals(198.0, size.width, 0.0001)
            assertEquals(255.12, size.height, 0.0001)
        }
        withContext(Dispatchers.Main) {
            val viewer = PdfSignatureViewer(instrumentation.targetContext)
            try {
                viewer.loadDocument(PdfSource.BytesSource(bytes))
                val ids =
                    (1..expectedPages).associate { page ->
                        val size = viewer.getPageSize(page)
                        Log.i("CoordinateRegression", "page=$page native=${size.width}x${size.height} exact=${exact[page - 1]}")
                        assertEquals(198.0, size.width, 0.0001)
                        assertTrue(kotlin.math.abs(size.height - 255.12) < 1)
                        viewer.placeSignature(PlaceOptions(page = page, x = 0.0, y = 0.0, width = 25.0, height = 46.0)) to "test-$page"
                    }
                val exported = viewer.exportInitSignDocument(ids, exact)
                assertArrayEquals(bytes, Base64.decode(exported.base64Pdf, Base64.DEFAULT))
                assertEquals(expectedPages, exported.signatureData.size)
                exported.signatureData.forEach { row ->
                    assertEquals(198.0, row["docWidth"] as Double, 0.0001)
                    assertEquals(255.12, row["docHeight"] as Double, 0.0001)
                    assertEquals(0.0, row["x"] as Double, 0.0001)
                    val top = (row["y"] as Double) + (row["height"] as Double)
                    assertEquals(255.12, top, 0.0001)
                }
                Log.i(
                    "CoordinateRegression",
                    "PASS original bytes and ${exported.signatureData.size} top-left placements; hash=${exported.documentSha256}",
                )
            } finally {
                viewer.destroy()
            }
        }
    }

    // A genuine fractional-size PDF, independent from Android's integer PageInfo builder.
    private fun smallPdf(
        width: Double = 198.0,
        height: Double = 255.12,
    ): ByteArray {
        val objects =
            listOf(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Count 1 /Kids [3 0 R] >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] /Contents 4 0 R >>",
                "<< /Length 0 >>\nstream\n\nendstream",
            )
        val content = StringBuilder("%PDF-1.4\n")
        val offsets =
            objects.mapIndexed { index, obj ->
                content.length.also { content.append("${index + 1} 0 obj\n$obj\nendobj\n") }
            }
        val xref = content.length
        content.append("xref\n0 5\n0000000000 65535 f \n")
        offsets.forEach { content.append("%010d 00000 n \n".format(it)) }
        content.append("trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return content.toString().toByteArray(Charsets.US_ASCII)
    }
}
