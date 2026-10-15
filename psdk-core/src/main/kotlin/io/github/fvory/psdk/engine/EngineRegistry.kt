package io.github.fvory.psdk.engine

import io.github.fvory.psdk.error.PsdkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.InvocationTargetException

/**
 * Selects the platform renderer and falls back to the optional Pdfium module for old devices.
 * Pdfium is found by class name so psdk-core has no dependency on it; psdk-pdfium's
 * consumer-rules.pro keeps that name in minified apps.
 */
internal object EngineRegistry {
    suspend fun open(
        file: File,
        password: String?,
    ): DocumentEngine {
        try {
            return PdfRendererEngine.open(file, password)
        } catch (e: PsdkException.PasswordNotSupported) {
            return openWithOptionalPdfium(file, password) ?: throw e
        }
    }

    private suspend fun openWithOptionalPdfium(
        file: File,
        password: String?,
    ): DocumentEngine? {
        val method =
            try {
                Class.forName(PDFIUM_ENGINE_CLASS).getMethod("open", File::class.java, String::class.java)
            } catch (_: ClassNotFoundException) {
                return null
            } catch (_: NoSuchMethodException) {
                return null
            }
        return try {
            // Pdfium parses the whole file: never on the caller's (main) thread
            withContext(Dispatchers.IO) { method.invoke(null, file, password) as DocumentEngine }
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            if (cause?.javaClass?.name == PDFIUM_PASSWORD_EXCEPTION) {
                throw PasswordRequiredException(cause)
            }
            throw PsdkException.Corrupted(cause ?: e)
        }
    }

    private const val PDFIUM_ENGINE_CLASS = "io.github.fvory.psdk.pdfium.PdfiumEngine"
    private const val PDFIUM_PASSWORD_EXCEPTION = "io.github.fvory.psdk.pdfium.PdfiumPasswordRequiredException"
}
