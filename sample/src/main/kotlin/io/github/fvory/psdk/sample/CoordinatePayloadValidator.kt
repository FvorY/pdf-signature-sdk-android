package io.github.fvory.psdk.sample

import io.github.fvory.psdk.model.PageSize
import org.json.JSONObject
import kotlin.math.abs

internal object CoordinatePayloadValidator {
    fun validate(
        payload: JSONObject,
        actualPages: List<PageSize>,
    ) {
        val rows = payload.getJSONArray("signatureData")
        require(rows.length() > 0) { "signatureData kosong" }
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val number = row.getDouble("page")
            require(number.isFinite() && number == number.toInt().toDouble()) { "Nomor halaman tidak valid" }
            val page = number.toInt()
            require(page in 1..actualPages.size) { "Halaman $page tidak ada di PDF" }
            val actual = actualPages[page - 1]
            val dw = row.getDouble("docWidth")
            val dh = row.getDouble("docHeight")
            require(abs(dw - actual.width) < 0.001 && abs(dh - actual.height) < 0.001) {
                "Halaman $page: PDF asli ${actual.width} x ${actual.height}, tetapi payload $dw x $dh. " +
                    "Gunakan ekspor JSON lengkap dari PDF yang sama."
            }
            val x = row.getDouble("x")
            val y = row.getDouble("y")
            val w = row.getDouble("width")
            val h = row.getDouble("height")
            require(listOf(x, y, w, h).all { it.isFinite() } && w > 0 && h > 0) { "Kotak TTD tidak valid" }
            require(x >= 0 && y >= 0 && x + w <= actual.width + 0.000001 && y + h <= actual.height + 0.000001) {
                "TTD ${index + 1} berada di luar halaman $page (${actual.width} x ${actual.height})"
            }
        }
    }
}
