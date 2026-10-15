package io.github.fvory.psdk.pdfium

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/** On a device: Pdfium page sizes follow /Rotate and CropBox, and regions match whole-page renders. */
@RunWith(AndroidJUnit4::class)
class PdfiumEngineTest {
    /** Web SDK sizes of geometry.pdf, rounded down like the platform renderers. */
    private val expectedSizes =
        listOf(595 to 841, 841 to 595, 595 to 841, 841 to 595, 600 to 800, 800 to 600, 612 to 792)

    @Test
    fun pageSizesFollowRotationAndCropBox() {
        val engine = PdfiumEngine.open(geometryPdf(), null)
        try {
            assertEquals(expectedSizes.size, engine.pageCount)
            expectedSizes.forEachIndexed { index, (width, height) ->
                val size = engine.pageSize(index + 1)
                assertEquals("page ${index + 1} width", width.toDouble(), size.width, 1.0)
                assertEquals("page ${index + 1} height", height.toDouble(), size.height, 1.0)
            }
        } finally {
            engine.close()
        }
    }

    @Test
    fun regionMatchesWholePageOnEveryGeometryPage() =
        runBlocking {
            val engine = PdfiumEngine.open(geometryPdf(), null)
            try {
                for (page in 1..engine.pageCount) {
                    val size = engine.pageSize(page)
                    val fullWidth = (size.width * 1.5).roundToInt()
                    val fullHeight = (size.height * 1.5).roundToInt()
                    val whole = engine.render(page, fullWidth, fullHeight)
                    val left = fullWidth / 4
                    val top = fullHeight / 3
                    val region = engine.renderRegion(page, fullWidth, fullHeight, left, top, fullWidth / 2, fullHeight / 3)
                    assertTrue("page $page: no region support", region != null)
                    assertSameArea("page $page", whole, region!!, left, top)
                }
            } finally {
                engine.close()
            }
        }

    private fun geometryPdf(): File {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.cacheDir, "geometry-pdfium-test.pdf")
        instrumentation.context.assets.open("geometry.pdf").use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }

    private fun assertSameArea(
        where: String,
        whole: Bitmap,
        region: Bitmap,
        left: Int,
        top: Int,
    ) {
        var different = 0
        var inked = 0
        for (y in 0 until region.height) {
            for (x in 0 until region.width) {
                val a = region.getPixel(x, y)
                val b = whole.getPixel(left + x, top + y)
                if (a != Color.WHITE) inked++
                val delta = maxOf(abs(Color.red(a) - Color.red(b)), abs(Color.green(a) - Color.green(b)), abs(Color.blue(a) - Color.blue(b)))
                if (delta > 48) different++
            }
        }
        val total = region.width * region.height
        assertTrue("$where: region is blank", inked > total / 200)
        assertTrue("$where: ${100.0 * different / total}% of pixels differ", different <= total / 100)
    }
}
