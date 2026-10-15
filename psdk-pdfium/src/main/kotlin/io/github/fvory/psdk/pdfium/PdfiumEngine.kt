package io.github.fvory.psdk.pdfium

import android.graphics.Bitmap
import android.graphics.Color
import android.os.ParcelFileDescriptor
import io.github.fvory.psdk.engine.DocumentEngine
import io.github.fvory.psdk.model.PageSize
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import io.legere.pdfiumandroid.api.PdfPasswordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * Optional Pdfium renderer, used when the platform renderer cannot open a password-protected PDF
 * (Android 11 and lower, or 12–14 without the PdfRendererPreV system update).
 *
 * The core module finds [open] by reflection, so `consumer-rules.pro` keeps its name. Pdfium is
 * not thread-safe: opening, rendering and closing all run on one background thread, one at a time.
 */
public object PdfiumEngine {
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "psdk-pdfium").apply { isDaemon = true } }
    internal val dispatcher = executor.asCoroutineDispatcher()
    internal val closeScope = CoroutineScope(SupervisorJob() + dispatcher)

    /**
     * Opens [file] with Pdfium and reads every page size, on the Pdfium thread. Blocks the caller
     * until done, so call it from a background thread.
     *
     * @throws PdfiumPasswordRequiredException when a password is needed or [password] is wrong.
     */
    @JvmStatic
    public fun open(
        file: File,
        password: String?,
    ): DocumentEngine =
        try {
            executor.submit(Callable { openNow(file, password) }).get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }

    private fun openNow(
        file: File,
        password: String?,
    ): DocumentEngine {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val document =
            try {
                PdfiumCore().newDocument(descriptor, password)
            } catch (e: PdfPasswordException) {
                descriptor.close()
                throw PdfiumPasswordRequiredException(e)
            } catch (e: Exception) {
                descriptor.close()
                throw e
            }
        return try {
            // Pdfium already reports the size after /Rotate and from the CropBox, like pdf.js and
            // PdfRenderer, so there is no width/height swap here.
            val sizes =
                List(document.getPageCount()) { index ->
                    (document.openPage(index) ?: throw IOException("Unable to open page ${index + 1}")).use { page ->
                        PageSize(page.getPageWidthPoint().toDouble(), page.getPageHeightPoint().toDouble())
                    }
                }
            PdfiumDocumentEngine(document, descriptor, sizes)
        } catch (e: Exception) {
            document.close()
            descriptor.close()
            throw e
        }
    }
}

/** Marker exception translated by the core engine registry into the SDK password flow. */
public class PdfiumPasswordRequiredException(
    cause: Throwable,
) : IOException("The PDF requires a password.", cause)

private class PdfiumDocumentEngine(
    private val document: PdfDocument,
    private val descriptor: ParcelFileDescriptor,
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
        withContext(PdfiumEngine.dispatcher) {
            mutex.withLock {
                check(!closed) { "Pdfium document is closed" }
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                (document.openPage(page - 1) ?: throw IOException("Unable to open page $page")).use { opened ->
                    opened.renderPageBitmap(bitmap, 0, 0, width, height)
                }
                bitmap
            }
        }

    /** Waits for a running render before freeing native memory, so closing mid-render cannot crash. */
    override fun close() {
        PdfiumEngine.closeScope.launch {
            mutex.withLock {
                if (closed) return@withLock
                closed = true
                document.close()
                descriptor.close()
            }
        }
    }
}
