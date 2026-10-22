package io.github.fvory.psdk.sample

import io.github.fvory.psdk.model.InitSignDocument
import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.SignatureItem
import io.github.fvory.psdk.model.toInitSignData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinatePayloadTest {
    @Test fun postmanBodyPreservesSourceAndCoordinatesAndApiNulls() {
        val row =
            SignatureItem("sdk-1", ItemType.SIGNATURE, 12.0, 20.0, 1, "TTD", 80.0, 40.0)
                .toInitSignData(PageSize(198.0, 255.12), PageSize(198.0, 255.12), "QR_test")
        val doc = InitSignDocument("original-pdf-base64", "local-hash", listOf(row))
        val json = CoordinatePayload.build(doc, "same.pdf", "Approved", "Surabaya", "123456")
        assertEquals("original-pdf-base64", json.getString("base64Pdf"))
        assertEquals("same.pdf", json.getString("pdfName"))
        assertEquals("123456", json.getString("otp"))
        assertEquals("otp", json.getString("otpType"))
        assertEquals("single", json.getString("signingType"))
        assertTrue(json.isNull("pdfPassword"))
        assertTrue(json.isNull("pdfNewPassword"))
        assertTrue(json.isNull("userList"))
        assertFalse(json.has("documentSha256"))
        val signature = json.getJSONArray("signatureData").getJSONObject(0)
        assertEquals(12.0, signature.getDouble("x"), 1e-9)
        assertEquals(195.12, signature.getDouble("y"), 1e-9)
        assertEquals(198.0, signature.getDouble("docWidth"), 1e-9)
        assertEquals(255.12, signature.getDouble("docHeight"), 1e-9)
        assertEquals("QR_test", signature.getString("specimenId"))
    }
}
