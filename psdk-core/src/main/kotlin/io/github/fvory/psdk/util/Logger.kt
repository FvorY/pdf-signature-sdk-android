package io.github.fvory.psdk.util

import android.util.Log
import io.github.fvory.psdk.config.PsdkLogLevel

internal class Logger(
    var level: PsdkLogLevel,
) {
    fun e(
        message: String,
        error: Throwable? = null,
    ) {
        if (level >= PsdkLogLevel.ERROR) Log.e(TAG, message, error)
    }

    fun d(message: String) {
        if (level >= PsdkLogLevel.DEBUG) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "PdfSignatureSDK"
    }
}
