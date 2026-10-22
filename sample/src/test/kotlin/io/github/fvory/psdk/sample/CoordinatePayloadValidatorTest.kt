package io.github.fvory.psdk.sample

import io.github.fvory.psdk.model.PageSize
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test

class CoordinatePayloadValidatorTest {
    private fun body(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
        dw: Double,
        dh: Double,
    ): JSONObject =
        JSONObject().put(
            "signatureData",
            org.json.JSONArray().put(
                JSONObject()
                    .put("page", 1)
                    .put("x", x)
                    .put("y", y)
                    .put("width", w)
                    .put("height", h)
                    .put("docWidth", dw)
                    .put("docHeight", dh),
            ),
        )

    @Test fun rejectsExactReportedPayloadAgainstItsActualPdf() {
        val payload = body(480.849144935608, 719.314264721341, 114.150855064392, 121.685735278659, 595.0, 841.0)
        assertThrows(IllegalArgumentException::class.java) {
            CoordinatePayloadValidator.validate(payload, listOf(PageSize(198.0, 255.12)))
        }
    }

    @Test fun acceptsProportionalCorrectionWithinActualPage() {
        val payload = body(160.0136650373956, 218.2062487701647, 37.986334962604396, 36.9137512298353, 198.0, 255.12)
        CoordinatePayloadValidator.validate(payload, listOf(PageSize(198.0, 255.12)))
    }

    @Test fun rejectsOffPageRectangleEvenWhenDocumentDimensionsMatch() {
        assertThrows(IllegalArgumentException::class.java) {
            CoordinatePayloadValidator.validate(body(480.0, 20.0, 80.0, 40.0, 198.0, 255.12), listOf(PageSize(198.0, 255.12)))
        }
    }
}
