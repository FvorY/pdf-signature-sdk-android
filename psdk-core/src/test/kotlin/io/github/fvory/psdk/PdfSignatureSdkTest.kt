package io.github.fvory.psdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfSignatureSdkTest {
    @Test
    fun versionIsSet() {
        // JitPack may build with a tag, a commit hash or "main-SNAPSHOT", so only check it is present.
        assertTrue(PdfSignatureSdk.version.isNotBlank())
    }

    @Test
    fun mirrorsWebSdk112() {
        assertEquals("1.1.2", PdfSignatureSdk.WEB_PARITY_VERSION)
    }
}
