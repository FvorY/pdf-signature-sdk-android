package io.github.fvory.psdk.engine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * On a device: a region rendered with [PdfRendererEngine.renderRegion] must match the same area of a
 * whole-page render, on every page of geometry.pdf (/Rotate 0/90/180/270, CropBox, offset MediaBox).
 * Otherwise the sharp detail drawn at high zoom would be shifted against the page underneath.
 */
@RunWith(AndroidJUnit4::class)
class RenderRegionTest {
    @Test
    fun regionMatchesWholePageOnEveryGeometryPage() =
        runBlocking {
            val engine = PdfRendererEngine.open(geometryPdf())
            try {
                for (page in 1..engine.pageCount) {
                    val size = engine.pageSize(page)
                    val fullWidth = (size.width * 1.5).roundToInt()
                    val fullHeight = (size.height * 1.5).roundToInt()
                    val whole = engine.render(page, fullWidth, fullHeight)
                    val left = fullWidth / 4
                    val top = fullHeight / 3
                    val region = engine.renderRegion(page, fullWidth, fullHeight, left, top, fullWidth / 2, fullHeight / 3)
                    assertSameArea("page $page", whole, region, left, top)
                }
            } finally {
                engine.close()
            }
        }

    companion object {
        fun geometryPdf(): File {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val file = File(instrumentation.targetContext.cacheDir, "geometry-test.pdf")
            instrumentation.context.assets.open("geometry.pdf").use { input -> file.outputStream().use { input.copyTo(it) } }
            return file
        }

        /** At most 1% of pixels may differ noticeably (anti-aliasing on line edges). */
        fun assertSameArea(
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
                    val delta =
                        maxOf(
                            abs(Color.red(a) - Color.red(b)),
                            abs(Color.green(a) - Color.green(b)),
                            abs(Color.blue(a) - Color.blue(b)),
                        )
                    if (delta > 48) different++
                }
            }
            val total = region.width * region.height
            assertTrue("$where: region is blank", inked > total / 200)
            assertTrue("$where: ${100.0 * different / total}% of pixels differ", different <= total / 100)
        }
    }
}
