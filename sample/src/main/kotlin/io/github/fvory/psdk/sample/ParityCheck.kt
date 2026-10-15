package io.github.fvory.psdk.sample

import android.content.Context
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import org.json.JSONObject
import kotlin.math.abs

/**
 * On-device web parity check (spec §20): for every PDF in assets/parity/scenarios.json, replays the
 * scenarios on the real PdfRenderer and compares page sizes and items with the web SDK output in
 * assets/parity/<name>.json. Returns one line per failure, or an empty list when everything
 * matches. Ends with a freshly loaded sample.pdf, so no test items are left on the pages.
 */
internal object ParityCheck {
    suspend fun run(
        context: Context,
        viewer: PdfSignatureViewer,
    ): List<String> {
        val config =
            JSONObject(
                context.assets
                    .open("parity/scenarios.json")
                    .bufferedReader()
                    .readText(),
            )
        val tolerance = config.getDouble("tolerance")
        val failures = mutableListOf<String>()
        val pdfs = config.getJSONArray("pdfs")
        for (p in 0 until pdfs.length()) {
            val pdf = pdfs.getString(p)
            val name = pdf.removeSuffix(".pdf")
            val expected =
                JSONObject(
                    context.assets
                        .open("parity/$name.json")
                        .bufferedReader()
                        .readText(),
                )
            failures += checkPdf(viewer, pdf, config, expected, tolerance).map { "$name: $it" }
        }
        viewer.loadDocument(PdfSource.AssetSource("sample.pdf"))
        return failures
    }

    private suspend fun checkPdf(
        viewer: PdfSignatureViewer,
        pdf: String,
        config: JSONObject,
        expected: JSONObject,
        tolerance: Double,
    ): List<String> {
        val failures = mutableListOf<String>()
        viewer.loadDocument(PdfSource.AssetSource(pdf))
        val sizes = expected.getJSONArray("pageSizes")
        if (sizes.length() != viewer.totalPages) failures += "${viewer.totalPages} pages, web ${sizes.length()}"
        for (page in 1..minOf(viewer.totalPages, sizes.length())) {
            val webSize = sizes.getJSONArray(page - 1)
            val size = viewer.getPageSize(page)
            if (abs(size.width - webSize.getDouble(0)) > tolerance || abs(size.height - webSize.getDouble(1)) > tolerance) {
                failures +=
                    "page $page size ${size.width.f()} × ${size.height.f()}, web ${webSize.getDouble(0).f()} × ${webSize.getDouble(1).f()}"
            }
        }

        val scenarios = config.getJSONArray("scenarios")
        for (s in 0 until scenarios.length()) {
            val scenario = scenarios.getJSONObject(s)
            if (!runsFor(scenario, pdf)) continue
            val name = scenario.getString("name")
            viewer.loadDocument(PdfSource.AssetSource(pdf))
            val actions = scenario.getJSONArray("actions")
            for (a in 0 until actions.length()) {
                val action = actions.getJSONObject(a)
                when (action.getString("op")) {
                    "goToPage" -> viewer.goToPage(action.getInt("page"))
                    "placeSignature" -> viewer.placeSignature(options(action.getJSONObject("opts")))
                    "placeEStamp" -> viewer.placeEStamp(options(action.getJSONObject("opts")))
                }
            }
            val wanted = expected.getJSONObject("scenarios").getJSONArray(name)
            val actual = viewer.getSignatures()
            if (wanted.length() != actual.size) {
                failures += "$name: ${actual.size} items, web ${wanted.length()}"
                continue
            }
            actual.forEachIndexed { index, item ->
                val web = wanted.getJSONObject(index)
                val map = item.toMap()
                for (key in listOf("type", "page", "label", "disableDrag", "disableDragging", "disableResize", "locked")) {
                    if (web.get(key).toString() !=
                        map.getValue(key).toString()
                    ) {
                        failures += "$name #$index $key: ${map[key]} ≠ ${web.get(key)}"
                    }
                }
                for (key in listOf("x", "y", "width", "height")) {
                    val value = map.getValue(key) as Double
                    if (abs(web.getDouble(key) - value) >
                        tolerance
                    ) {
                        failures += "$name #$index $key: ${value.f()} ≠ ${web.getDouble(key).f()}"
                    }
                }
            }
        }
        return failures
    }

    private fun runsFor(
        scenario: JSONObject,
        pdf: String,
    ): Boolean {
        val only = scenario.optJSONArray("only") ?: return true
        return (0 until only.length()).any { only.getString(it) == pdf }
    }

    private fun options(json: JSONObject): PlaceOptions =
        PlaceOptions(
            x = if (json.has("x")) json.getDouble("x") else null,
            y = if (json.has("y")) json.getDouble("y") else null,
            page = if (json.has("page")) json.getInt("page") else null,
            label = if (json.has("label")) json.getString("label") else null,
            width = if (json.has("width")) json.getDouble("width") else null,
            height = if (json.has("height")) json.getDouble("height") else null,
            disableDrag = json.optBoolean("disableDrag"),
            disableDragging = json.optBoolean("disableDragging"),
            disableResize = json.optBoolean("disableResize"),
            locked = json.optBoolean("locked"),
        )

    private fun Double.f(): String = "%.2f".format(this)
}
