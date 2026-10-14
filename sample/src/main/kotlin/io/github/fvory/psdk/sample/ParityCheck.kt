package io.github.fvory.psdk.sample

import android.content.Context
import io.github.fvory.psdk.PdfSignatureViewer
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PlaceOptions
import org.json.JSONObject
import kotlin.math.abs

/**
 * On-device web parity check (spec §20): replays assets/parity/scenarios.json on the real
 * PdfRenderer and compares with the web SDK output in assets/parity/sample.json.
 * Returns one line per failure, or an empty list when everything matches.
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
        val expected =
            JSONObject(
                context.assets
                    .open("parity/sample.json")
                    .bufferedReader()
                    .readText(),
            )
        val tolerance = config.getDouble("tolerance")
        val failures = mutableListOf<String>()

        viewer.loadDocument(PdfSource.AssetSource("sample.pdf"))
        val sizes = expected.getJSONArray("pageSizes")
        for (page in 1..viewer.totalPages) {
            val webSize = sizes.getJSONArray(page - 1)
            val size = viewer.getPageSize(page)
            if (abs(size.width - webSize.getDouble(0)) > tolerance || abs(size.height - webSize.getDouble(1)) > tolerance) {
                failures += "page $page size ${size.width} × ${size.height}, web ${webSize.getDouble(0)} × ${webSize.getDouble(1)}"
            }
        }

        val scenarios = config.getJSONArray("scenarios")
        for (s in 0 until scenarios.length()) {
            val scenario = scenarios.getJSONObject(s)
            val name = scenario.getString("name")
            viewer.loadDocument(PdfSource.AssetSource("sample.pdf"))
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
                    val difference = abs(web.getDouble(key) - map.getValue(key) as Double)
                    if (difference > tolerance) failures += "$name #$index $key: ${map[key]} ≠ ${web.getDouble(key)}"
                }
            }
        }
        return failures
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
}
