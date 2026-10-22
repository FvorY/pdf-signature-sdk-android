package io.github.fvory.psdk.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InitSignDocumentTest {
    private fun item(
        x: Double = 12.0,
        y: Double = 20.0,
        width: Double = 80.0,
        height: Double = 40.0,
    ) = SignatureItem("sdk-1", ItemType.SIGNATURE, x, y, 1, "Direktur", width, height)

    @Test fun matchesFlutterPreviewConversionAtDifferentPreviewSizes() {
        val page = PageSize(198.0, 255.12)
        for (previewWidth in listOf(198.0, 390.0, 720.0)) {
            val previewHeight = previewWidth * page.height / page.width
            val i = item()
            val mobileX = (i.x + i.width / 2 - page.width / 2) * previewWidth / page.width
            val mobileY = (i.y + i.height / 2 - page.height / 2) * previewHeight / page.height
            val mobileWidth = i.width * previewWidth / page.width
            val mobileHeight = i.height * previewHeight / page.height
            // Original Flutter previewSpecimenToPage, including its center-origin preview.
            val flutterX = (previewWidth / 2 + mobileX) * page.width / previewWidth - mobileWidth * page.width / previewWidth / 2
            val flutterY =
                page.height - (previewHeight / 2 + mobileY) * page.height / previewHeight - mobileHeight * page.height / previewHeight / 2
            val row = i.toInitSignData(page, page, "QR_test")
            assertEquals(flutterX, row["x"] as Double, 1e-9)
            assertEquals(flutterY, row["y"] as Double, 1e-9)
            assertEquals(i.width, row["width"] as Double, 1e-9)
            assertEquals(i.height, row["height"] as Double, 1e-9)
            assertEquals(page.width, row["docWidth"])
            assertEquals(page.height, row["docHeight"])
        }
    }

    @Test fun normalizesAndroidIntegerPageSizeToExactPdfSize() {
        val row = item().toInitSignData(PageSize(198.0, 255.0), PageSize(198.0, 255.12), "QR_test")
        assertEquals(195.0 * 255.12 / 255.0, row["y"] as Double, 1e-9)
        assertEquals(40.0 * 255.12 / 255.0, row["height"] as Double, 1e-9)
    }

    @Test fun rejectsReportedIncidentInsteadOfMovingSignature() {
        assertThrows(IllegalArgumentException::class.java) {
            item(409.3586120605469, 58.96342468261719, 160.0, 48.0)
                .toInitSignData(PageSize(198.0, 255.12), PageSize(198.0, 255.12), "QR_test")
        }
        assertThrows(IllegalArgumentException::class.java) {
            item().toInitSignData(PageSize(595.0, 842.0), PageSize(198.0, 255.12), "QR_test")
        }
    }

    @Test fun rejectsInvalidRectanglesAndMissingBackendId() {
        for (i in listOf(item(Double.NaN), item(-1.0), item(width = 0.0), item(y = 240.0))) {
            assertThrows(IllegalArgumentException::class.java) {
                i.toInitSignData(PageSize(198.0, 255.12), PageSize(198.0, 255.12), "QR_test")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            item().toInitSignData(PageSize(198.0, 255.12), PageSize(198.0, 255.12), "")
        }
    }

    @Test fun convertsSdkEstampToFlutterEmeteraiType() {
        val row =
            item()
                .copy(type = ItemType.ESTAMP)
                .toInitSignData(PageSize(198.0, 255.12), PageSize(198.0, 255.12), "materai-id")
        assertEquals("emeterai", row["specimenType"])
    }

    @Test fun exportsApiKeysAndExplicitNullWithoutLocalHash() {
        val row = item().toInitSignData(PageSize(198.0, 255.12), PageSize(198.0, 255.12), "QR_test")
        val json = InitSignDocument("same-source-base64", "local-hash", listOf(row)).toJson()
        assertFalse(json.has("documentSha256"))
        assertEquals("same-source-base64", json.getString("base64Pdf"))
        val signature = json.getJSONArray("signatureData").getJSONObject(0)
        assertTrue(signature.has("jenisDocument"))
        assertTrue(signature.isNull("jenisDocument"))
        assertEquals("QR_test", signature.getString("specimenId"))
        assertEquals(1, signature.getInt("page"))
    }
}
