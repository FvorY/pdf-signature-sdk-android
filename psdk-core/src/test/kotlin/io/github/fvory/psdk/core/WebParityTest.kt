package io.github.fvory.psdk.core

import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.PlaceOptions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Spec §20: replays parity/scenarios.json with the page sizes the web SDK measured and compares
 * with the files in parity/expected, which the real web SDK 1.1.2 produced (parity/web).
 * The same comparison runs on a device, with PdfRenderer's page sizes, from the sample app.
 */
class WebParityTest {
    private val parityDir = File("../parity")
    private val config = JSONObject(File(parityDir, "scenarios.json").readText())
    private val tolerance = config.getDouble("tolerance")

    @Test
    fun placementMatchesWebSdk() {
        val pdfs = config.getJSONArray("pdfs")
        for (p in 0 until pdfs.length()) {
            val pdf = pdfs.getString(p)
            val expected = JSONObject(File(parityDir, "expected/${pdf.removeSuffix(".pdf")}.json").readText())
            val sizes = expected.getJSONArray("pageSizes").let { array -> List(array.length()) { array.getJSONArray(it) } }
            val scenarios = config.getJSONArray("scenarios")
            for (s in 0 until scenarios.length()) {
                val scenario = scenarios.getJSONObject(s)
                val name = scenario.getString("name")
                val actual =
                    replay(scenario.getJSONArray("actions")) { page -> sizes[page - 1].let { PageSize(it.getDouble(0), it.getDouble(1)) } }
                val wanted = expected.getJSONObject("scenarios").getJSONArray(name)
                assertEquals("$pdf / $name: item count", wanted.length(), actual.size)
                for (i in actual.indices) compare("$pdf / $name #$i", wanted.getJSONObject(i), actual[i])
            }
        }
    }

    @Test
    fun sampleAppCopyIsUpToDate() {
        val copy = File("../sample/src/main/assets/parity")
        assertEquals(File(parityDir, "scenarios.json").readText(), File(copy, "scenarios.json").readText())
        assertEquals(File(parityDir, "expected/sample.json").readText(), File(copy, "sample.json").readText())
    }

    private fun replay(
        actions: JSONArray,
        pageSize: (Int) -> PageSize,
    ): List<io.github.fvory.psdk.model.SignatureItem> {
        val store = ItemStore(IdGenerator(), onChanged = {}, emit = {})
        var currentPage = 1
        for (a in 0 until actions.length()) {
            val action = actions.getJSONObject(a)
            when (val op = action.getString("op")) {
                "goToPage" -> currentPage = action.getInt("page")
                "placeSignature", "placeEStamp" -> {
                    val options = parseOptions(action.getJSONObject("opts"))
                    val page = options.page ?: currentPage
                    store.place(if (op == "placeEStamp") ItemType.ESTAMP else ItemType.SIGNATURE, options, page, pageSize(page))
                }
                else -> error("Unknown op $op")
            }
        }
        return store.all()
    }

    private fun compare(
        where: String,
        wanted: JSONObject,
        actual: io.github.fvory.psdk.model.SignatureItem,
    ) {
        val map = actual.toMap()
        for (key in listOf("type", "page", "label", "disableDrag", "disableDragging", "disableResize", "locked")) {
            assertEquals("$where: $key", wanted.get(key).toString(), map.getValue(key).toString())
        }
        for (key in listOf("x", "y", "width", "height")) {
            val difference = kotlin.math.abs(wanted.getDouble(key) - (map.getValue(key) as Double))
            assertTrue("$where: $key differs by $difference", difference <= tolerance)
        }
    }

    companion object {
        fun parseOptions(json: JSONObject): PlaceOptions =
            PlaceOptions(
                x = json.optDoubleOrNull("x"),
                y = json.optDoubleOrNull("y"),
                page = if (json.has("page")) json.getInt("page") else null,
                label = if (json.has("label")) json.getString("label") else null,
                width = json.optDoubleOrNull("width"),
                height = json.optDoubleOrNull("height"),
                disableDrag = json.optBoolean("disableDrag"),
                disableDragging = json.optBoolean("disableDragging"),
                disableResize = json.optBoolean("disableResize"),
                locked = json.optBoolean("locked"),
            )

        private fun JSONObject.optDoubleOrNull(key: String): Double? = if (has(key)) getDouble(key) else null
    }
}
