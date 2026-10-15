package io.github.fvory.psdk.engine

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.LoadParams
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRendererPreV
import android.graphics.pdf.RenderParams
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ext.SdkExtensions
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
 * [DocumentEngine] on the platform PDF renderers.
 *
 * - No password: `PdfRenderer` on every Android version.
 * - Password, Android 15+: `PdfRenderer` with `LoadParams`.
 * - Password, Android 12–14: `PdfRendererPreV` with `LoadParams`, available when the device has
 *   the S extension level 13 system update. Otherwise the document needs `psdk-pdfium`.
 *
 * The renderers are not thread-safe and allow one open page at a time (spec §15.1 R4), so every
 * call runs on one shared background thread behind a mutex, and each page is closed right away.
 * Pages render on a white bitmap because the renderers leave the background transparent (R3).
 */
internal class PdfRendererEngine private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: Renderer,
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
                renderer.render(page - 1, bitmap)
                bitmap
            }
        }

    override suspend fun renderRegion(
        page: Int,
        fullWidth: Int,
        fullHeight: Int,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
    ): Bitmap =
        withContext(renderDispatcher) {
            mutex.withLock {
                if (closed) throw PsdkException.NotLoaded()
                val size = sizes[page - 1]
                // Same mapping the renderer uses for a whole page (points → pixels), shifted to the region
                val matrix =
                    Matrix().apply {
                        setScale((fullWidth / size.width).toFloat(), (fullHeight / size.height).toFloat())
                        postTranslate(-left.toFloat(), -top.toFloat())
                    }
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                renderer.render(page - 1, bitmap, matrix)
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

    /** The two platform renderer classes behind one shape. */
    private interface Renderer : AutoCloseable {
        val pageCount: Int

        fun pageSize(index: Int): PageSize

        fun render(
            index: Int,
            bitmap: Bitmap,
            transform: Matrix? = null,
        )
    }

    private class Platform(
        private val renderer: PdfRenderer,
    ) : Renderer {
        override val pageCount: Int get() = renderer.pageCount

        override fun pageSize(index: Int): PageSize = renderer.openPage(index).use { PageSize(it.width.toDouble(), it.height.toDouble()) }

        override fun render(
            index: Int,
            bitmap: Bitmap,
            transform: Matrix?,
        ) {
            renderer.openPage(index).use { it.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
        }

        override fun close() {
            renderer.close()
        }
    }

    @SuppressLint("NewApi")
    private class PreV(
        private val renderer: PdfRendererPreV,
    ) : Renderer {
        override val pageCount: Int get() = renderer.pageCount

        override fun pageSize(index: Int): PageSize = renderer.openPage(index).use { PageSize(it.width.toDouble(), it.height.toDouble()) }

        override fun render(
            index: Int,
            bitmap: Bitmap,
            transform: Matrix?,
        ) {
            renderer.openPage(index).use {
                it.render(bitmap, null, transform, RenderParams.Builder(RenderParams.RENDER_MODE_FOR_DISPLAY).build())
            }
        }

        override fun close() {
            renderer.close()
        }
    }

    companion object {
        private val renderDispatcher =
            Executors
                .newSingleThreadExecutor { runnable ->
                    Thread(runnable, "psdk-render").apply { isDaemon = true }
                }.asCoroutineDispatcher()
        private val closeScope = CoroutineScope(SupervisorJob() + renderDispatcher)

        /** Whether a platform renderer on this device can open password-protected PDFs. */
        @SuppressLint("NewApi")
        fun supportsPasswords(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM ||
                (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= PRE_V_EXTENSION_LEVEL
                )

        /**
         * Opens [file], with [password] when given, and reads every page size up front (spec §15.3).
         *
         * @throws PasswordRequiredException when a password is needed or [password] is wrong.
         * @throws PsdkException.PasswordNotSupported when the PDF needs a password this device cannot use.
         */
        suspend fun open(
            file: File,
            password: String? = null,
        ): PdfRendererEngine =
            withContext(renderDispatcher) {
                val descriptor =
                    try {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    } catch (e: IOException) {
                        throw PsdkException.InvalidSource("Cannot open $file", e)
                    }
                try {
                    val renderer = create(descriptor, password)
                    val sizes = List(renderer.pageCount) { renderer.pageSize(it) }
                    PdfRendererEngine(descriptor, renderer, sizes)
                } catch (e: SecurityException) {
                    descriptor.close()
                    if (supportsPasswords()) throw PasswordRequiredException(e)
                    throw PsdkException.PasswordNotSupported()
                } catch (e: PsdkException) {
                    descriptor.close()
                    throw e
                } catch (e: IOException) {
                    descriptor.close()
                    throw PsdkException.Corrupted(e)
                } catch (e: RuntimeException) {
                    descriptor.close()
                    throw PsdkException.Corrupted(e)
                }
            }

        @SuppressLint("NewApi")
        private fun create(
            descriptor: ParcelFileDescriptor,
            password: String?,
        ): Renderer {
            if (password == null) return Platform(PdfRenderer(descriptor))
            val params = LoadParams.Builder().setPassword(password).build()
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> Platform(PdfRenderer(descriptor, params))
                supportsPasswords() -> PreV(PdfRendererPreV(descriptor, params))
                else -> throw PsdkException.PasswordNotSupported()
            }
        }

        /** `PdfRendererPreV` ships with the S extension level 13 system update. */
        private const val PRE_V_EXTENSION_LEVEL = 13
    }
}

/** Raised by an engine when the document needs a password, or the given one is wrong. Handled inside the viewer. */
internal class PasswordRequiredException(
    cause: Throwable,
) : Exception(cause)
