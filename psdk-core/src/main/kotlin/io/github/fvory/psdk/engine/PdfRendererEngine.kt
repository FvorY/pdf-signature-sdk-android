package io.github.fvory.psdk.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import io.github.fvory.psdk.error.PsdkException
import io.github.fvory.psdk.model.PageSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * [DocumentEngine] on the platform `PdfRenderer`.
 *
 * `PdfRenderer` is not thread-safe and allows one open page at a time (spec §15.1 R4), so every
 * call runs on one shared background thread behind a mutex, and each page is closed right away.
 * Pages render on a white bitmap because `PdfRenderer` leaves the background transparent (R3).
 */
internal class PdfRendererEngine private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val sizes: List<PageSize>,
) : DocumentEngine {
    private val mutex = Mutex()
    private var closed = false

    override val pageCount: Int get() = sizes.size

    override fun pageSize(page: Int): PageSize = sizes[page - 1]

    override suspend fun render(
        page: Int,
        width: Int,
        height: Int,
    ): Bitmap =
        withContext(renderDispatcher) {
            mutex.withLock {
                if (closed) throw PsdkException.NotLoaded()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                renderer.openPage(page - 1).use { it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                bitmap
            }
        }

    override fun close() {
        closeScope.launch {
            mutex.withLock {
                if (closed) return@withLock
                closed = true
                renderer.close()
                descriptor.close()
            }
        }
    }

    companion object {
        private val renderDispatcher =
            Executors
                .newSingleThreadExecutor { runnable ->
                    Thread(runnable, "psdk-render").apply { isDaemon = true }
                }.asCoroutineDispatcher()
        private val closeScope = CoroutineScope(SupervisorJob() + renderDispatcher)

        /** Opens [file] and reads every page size up front (spec §15.3). */
        suspend fun open(file: File): PdfRendererEngine =
            withContext(renderDispatcher) {
                val descriptor =
                    try {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    } catch (e: IOException) {
                        throw PsdkException.InvalidSource("Cannot open $file", e)
                    }
                try {
                    val renderer = PdfRenderer(descriptor)
                    val sizes =
                        List(renderer.pageCount) { index ->
                            renderer.openPage(index).use { PageSize(it.width.toDouble(), it.height.toDouble()) }
                        }
                    PdfRendererEngine(descriptor, renderer, sizes)
                } catch (e: SecurityException) {
                    descriptor.close()
                    throw PasswordRequiredException(e)
                } catch (e: IOException) {
                    descriptor.close()
                    throw PsdkException.Corrupted(e)
                } catch (e: RuntimeException) {
                    descriptor.close()
                    throw PsdkException.Corrupted(e)
                }
            }
    }
}

/** Raised by an engine when the document needs a password. Handled inside the viewer. */
internal class PasswordRequiredException(
    cause: Throwable,
) : Exception(cause)
