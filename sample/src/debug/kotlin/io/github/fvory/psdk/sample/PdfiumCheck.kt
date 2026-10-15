package io.github.fvory.psdk.sample

import android.content.Context
import android.graphics.Color
import io.github.fvory.psdk.pdfium.PdfiumEngine
import io.github.fvory.psdk.pdfium.PdfiumPasswordRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

/**
 * Debug-only check of the optional Pdfium engine, which a modern phone never uses on its own:
 * page sizes of geometry.pdf against the web SDK, the password flow, rendering, and closing a
 * document while a page is still rendering.
 */
internal object PdfiumCheck {
    suspend fun run(context: Context): List<String> =
        withContext(Dispatchers.IO) {
            buildList {
                val geometry = copy(context, "geometry.pdf")
                val expected =
                    JSONObject(
                        context.assets
                            .open("parity/geometry.json")
                            .bufferedReader()
                            .readText(),
                    ).getJSONArray("pageSizes")
                val engine = PdfiumEngine.open(geometry, null)
                for (page in 1..engine.pageCount) {
                    val size = engine.pageSize(page)
                    val web = expected.getJSONArray(page - 1)
                    val ok = abs(size.width - web.getDouble(0)) <= 1.0 && abs(size.height - web.getDouble(1)) <= 1.0
                    add(
                        "${if (ok) "OK" else "BEDA"} p$page ${size.width.toInt()}×${size.height.toInt()} web ${web.getDouble(
                            0,
                        ).toInt()}×${web.getDouble(1).toInt()}",
                    )
                }
                val bitmap = engine.render(2, 300, 212)
                val inked =
                    (0 until bitmap.width step 3).any { x ->
                        (0 until bitmap.height step 3).any { y ->
                            bitmap.getPixel(x, y) !=
                                Color.WHITE
                        }
                    }
                add("${if (inked) "OK" else "BEDA"} render halaman 2 berisi gambar")
                engine.close()

                val password = copy(context, "password.pdf")
                add(passwordCase("tanpa password", password, null, expectPrompt = true))
                add(passwordCase("password salah", password, "salah", expectPrompt = true))
                add(passwordCase("password benar", password, "test123", expectPrompt = false))

                // Close while renders are queued: must not crash the app
                val busy = PdfiumEngine.open(geometry, null)
                coroutineScope {
                    val renders = (1..busy.pageCount).map { page -> async { runCatching { busy.render(page, 1200, 1600) } } }
                    delay(5)
                    busy.close()
                    renders.forEach { it.await() }
                }
                add("OK tutup saat render: tidak crash")
                geometry.delete()
                password.delete()
            }
        }

    private fun passwordCase(
        name: String,
        file: File,
        password: String?,
        expectPrompt: Boolean,
    ): String =
        try {
            val engine = PdfiumEngine.open(file, password)
            val pages = engine.pageCount
            engine.close()
            if (expectPrompt) "BEDA $name: terbuka tanpa minta password" else "OK $name: $pages halaman"
        } catch (e: PdfiumPasswordRequiredException) {
            if (expectPrompt) "OK $name: minta password" else "BEDA $name: masih minta password"
        }

    private fun copy(
        context: Context,
        asset: String,
    ): File {
        val file = File(context.cacheDir, "pdfium-check-$asset")
        context.assets.open(asset).use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }
}
