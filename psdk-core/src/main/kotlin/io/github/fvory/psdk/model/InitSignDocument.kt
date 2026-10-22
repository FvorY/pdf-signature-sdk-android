package io.github.fvory.psdk.model

import org.json.JSONArray
import org.json.JSONObject

/** Fields to merge into /signing/initsign. The source PDF is never flattened or regenerated. */
public data class InitSignDocument(
    public val base64Pdf: String,
    public val documentSha256: String,
    public val signatureData: List<Map<String, Any?>>,
) {
    public fun toJson(): JSONObject =
        JSONObject().apply {
            put("base64Pdf", base64Pdf)
            put(
                "signatureData",
                JSONArray().apply {
                    signatureData.forEach { row ->
                        put(JSONObject().apply { row.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) } })
                    }
                },
            )
        }
}

/** Converts SDK top-left page points to the same bottom-left PDF points as Flutter resizePdfView. */
public fun SignatureItem.toInitSignData(
    sdkPageSize: PageSize,
    pdfPageSize: PageSize,
    specimenId: String,
): Map<String, Any?> {
    require(specimenId.isNotBlank()) { "Missing backend specimenId for $id" }
    require(page >= 1) { "Invalid page for $id" }
    require(
        listOf(sdkPageSize.width, sdkPageSize.height, pdfPageSize.width, pdfPageSize.height)
            .all { it.isFinite() && it > 0 },
    ) { "Invalid page dimensions" }
    // Android PdfRenderer rounds page dimensions to integer points. Only this sub-point
    // discrepancy may be normalized; a different document/page size must never be silently scaled.
    require(
        kotlin.math.abs(sdkPageSize.width - pdfPageSize.width) < 1 &&
            kotlin.math.abs(sdkPageSize.height - pdfPageSize.height) < 1,
    ) { "SDK and payload page dimensions differ" }
    require(listOf(x, y, width, height).all { it.isFinite() } && width > 0 && height > 0) {
        "Invalid signature rectangle: $id"
    }
    require(x >= 0 && y >= 0 && x + width <= sdkPageSize.width && y + height <= sdkPageSize.height) {
        "Signature $id is outside page $page (${sdkPageSize.width} x ${sdkPageSize.height})"
    }
    val sx = pdfPageSize.width / sdkPageSize.width
    val sy = pdfPageSize.height / sdkPageSize.height
    return linkedMapOf(
        "viewOrientation" to 0,
        "x" to x * sx,
        "y" to (sdkPageSize.height - y - height) * sy,
        "width" to width * sx,
        "height" to height * sy,
        "docWidth" to pdfPageSize.width,
        "docHeight" to pdfPageSize.height,
        "page" to page,
        "scale" to 1,
        "specimenId" to specimenId,
        "specimenType" to if (type == ItemType.ESTAMP) "emeterai" else "signature",
        "jenisDocument" to null,
    )
}
