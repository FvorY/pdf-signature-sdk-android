package io.github.fvory.psdk.model

import io.github.fvory.psdk.cache.Base64Data
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** TC-U19, TC-U20 — B-OVL-08, spec Appendix C. */
class SignatureItemJsonTest {
    private val signature =
        SignatureItem(
            id = "signature-1760000000000-k3x9a",
            type = ItemType.SIGNATURE,
            x = 217.5,
            y = 397.0,
            page = 1,
            label = "Direktur",
            width = 160.0,
            height = 48.0,
        )

    @Test
    fun jsonMatchesWebJsonStringify() {
        assertEquals(
            """{"id":"signature-1760000000000-k3x9a","type":"signature","x":217.5,"y":397,"page":1,""" +
                """"label":"Direktur","width":160,"height":48,"disableDrag":false,"disableDragging":false,""" +
                """"disableResize":false,"locked":false}""",
            signature.toJsonString(),
        )
    }

    @Test
    fun listJson() {
        val stamp = signature.copy(id = "estamp-1-aaaaa", type = ItemType.ESTAMP, label = "E-Materai")
        val json = listOf(signature, stamp).toJsonString()
        assertEquals('[', json.first())
        assertEquals(2, Regex("\"id\"").findAll(json).count())
        assertEquals(true, json.contains("\"type\":\"estamp\""))
    }

    @Test
    fun mapHasWebKeysInOrderAndNoImage() {
        assertEquals(
            listOf("id", "type", "x", "y", "page", "label", "width", "height", "disableDrag", "disableDragging", "disableResize", "locked"),
            signature.toMap().keys.toList(),
        )
        assertFalse(signature.toMap().containsKey("image"))
    }

    @Test
    fun labelsAreEscaped() {
        val json = signature.copy(label = "Tanda \"Tangan\"\n").toJsonString()
        assertEquals(true, json.contains(""""label":"Tanda \"Tangan\"\n""""))
    }

    @Test
    fun numbersFormatLikeJavaScript() {
        assertEquals("397", JsonText.number(397.0))
        assertEquals("217.5", JsonText.number(217.5))
        assertEquals("-12", JsonText.number(-12.0))
        assertEquals("0.25", JsonText.number(0.25))
    }

    @Test
    fun yFromBottom() {
        assertEquals(842.0 - 397.0 - 48.0, signature.yFromBottom(842.0), 0.0)
    }

    @Test
    fun imageStringParsing() {
        assertEquals(PsdkImage.Url("https://contoh.id/ttd.png"), PsdkImage.of("https://contoh.id/ttd.png"))
        assertEquals(PsdkImage.Url("HTTP://x.id/a.png"), PsdkImage.of(" HTTP://x.id/a.png "))
        assertEquals(PsdkImage.Base64("data:image/png;base64,AAA"), PsdkImage.of("data:image/png;base64,AAA"))
        assertEquals(PsdkImage.Base64("iVBORw0KGgo"), PsdkImage.of("iVBORw0KGgo"))
    }

    @Test
    fun base64PayloadExtraction() {
        assertEquals("AAA=", Base64Data.payload("data:image/png;base64,AAA="))
        assertEquals("AAA=", Base64Data.payload("AAA="))
        assertEquals("", Base64Data.payload("data:image/png"))
    }
}
