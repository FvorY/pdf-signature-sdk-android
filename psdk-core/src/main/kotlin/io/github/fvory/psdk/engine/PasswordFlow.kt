package io.github.fvory.psdk.engine

import io.github.fvory.psdk.error.PsdkException

/**
 * Opening a document that may need a password (B-LOAD-04/05, B-PWD-03):
 * try without one; while the engine asks for a password, [ask] the user (with `isIncorrect` after
 * a wrong one) and retry. A `null` answer means the user cancelled.
 */
internal object PasswordFlow {
    suspend fun <T> open(
        open: suspend (password: String?) -> T,
        ask: suspend (isIncorrect: Boolean) -> String?,
    ): T {
        try {
            return open(null)
        } catch (e: PasswordRequiredException) {
            // fall through to the prompt
        }
        var isIncorrect = false
        while (true) {
            val password = ask(isIncorrect) ?: throw PsdkException.PasswordCancelled()
            try {
                return open(password)
            } catch (e: PasswordRequiredException) {
                isIncorrect = true
            }
        }
    }
}
