package io.github.fvory.psdk.engine

import io.github.fvory.psdk.error.PsdkException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** B-LOAD-04/05, B-PWD-03. */
class PasswordFlowTest {
    private val asked = mutableListOf<Boolean>()

    private fun opener(correct: String?): suspend (String?) -> String =
        { password ->
            if (correct != null && password != correct) throw PasswordRequiredException(SecurityException())
            "opened with $password"
        }

    @Test
    fun noPasswordNeededNeverAsks() =
        runBlocking {
            val result =
                PasswordFlow.open(opener(null)) {
                    asked += it
                    "x"
                }
            assertEquals("opened with null", result)
            assertTrue(asked.isEmpty())
        }

    @Test
    fun asksThenOpens() =
        runBlocking {
            val result =
                PasswordFlow.open(opener("test123")) {
                    asked += it
                    "test123"
                }
            assertEquals("opened with test123", result)
            assertEquals(listOf(false), asked)
        }

    @Test
    fun wrongPasswordAsksAgainAsIncorrect() =
        runBlocking {
            val answers = ArrayDeque(listOf("nope", "still wrong", "test123"))
            val result =
                PasswordFlow.open(opener("test123")) {
                    asked += it
                    answers.removeFirst()
                }
            assertEquals("opened with test123", result)
            assertEquals(listOf(false, true, true), asked)
        }

    @Test(expected = PsdkException.PasswordCancelled::class)
    fun cancelThrows(): Unit =
        runBlocking {
            PasswordFlow.open(opener("test123")) { null }
        }
}
