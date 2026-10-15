package io.github.fvory.psdk.state

import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.SignatureItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the viewer saves so it can come back after Android kills the app in the background
 * (spec §17, process death). Kept small because saved state is limited to about 1 MB: the PDF and
 * item images stay in the cache folder and only their paths are stored. Passwords are never saved.
 */
internal data class ViewerSnapshot(
    /** Folder name of the viewer's temp files, so the restored viewer keeps using it. */
    val tempId: String,
    /** Local copy of the open document; null when nothing was open. */
    val documentPath: String?,
    val page: Int,
    val scale: Float,
    val paginationLocked: Boolean,
    val scrollIndex: Int,
    val scrollOffset: Int,
    val scrollHorizontal: Int,
    val items: List<SignatureItem>,
    /** Item id → PNG of its loaded image in the temp folder. */
    val itemImages: Map<String, String>,
) {
    fun toJson(): String =
        JSONObject()
            .put("v", VERSION)
            .put("tempId", tempId)
            .put("documentPath", documentPath ?: JSONObject.NULL)
            .put("page", page)
            .put("scale", scale.toDouble())
            .put("paginationLocked", paginationLocked)
            .put("scroll", JSONArray().put(scrollIndex).put(scrollOffset).put(scrollHorizontal))
            .put("items", JSONArray().apply { items.forEach { put(JSONObject(it.toJsonString())) } })
            .put("itemImages", JSONObject(itemImages))
            .toString()

    companion object {
        private const val VERSION = 1

        /** Null when [json] is missing, from another version, or damaged: the viewer then starts fresh. */
        fun fromJson(json: String?): ViewerSnapshot? {
            if (json.isNullOrEmpty()) return null
            return try {
                val o = JSONObject(json)
                if (o.optInt("v") != VERSION) return null
                val scroll = o.getJSONArray("scroll")
                val items = o.getJSONArray("items")
                val images = o.getJSONObject("itemImages")
                ViewerSnapshot(
                    tempId = o.getString("tempId"),
                    documentPath = if (o.isNull("documentPath")) null else o.getString("documentPath"),
                    page = o.getInt("page"),
                    scale = o.getDouble("scale").toFloat(),
                    paginationLocked = o.getBoolean("paginationLocked"),
                    scrollIndex = scroll.getInt(0),
                    scrollOffset = scroll.getInt(1),
                    scrollHorizontal = scroll.getInt(2),
                    items = List(items.length()) { itemFromJson(items.getJSONObject(it)) },
                    itemImages = images.keys().asSequence().associateWith { images.getString(it) },
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun itemFromJson(o: JSONObject): SignatureItem =
            SignatureItem(
                id = o.getString("id"),
                type = ItemType.entries.first { it.value == o.getString("type") },
                x = o.getDouble("x"),
                y = o.getDouble("y"),
                page = o.getInt("page"),
                label = o.getString("label"),
                width = o.getDouble("width"),
                height = o.getDouble("height"),
                disableDrag = o.getBoolean("disableDrag"),
                disableDragging = o.getBoolean("disableDragging"),
                disableResize = o.getBoolean("disableResize"),
                locked = o.getBoolean("locked"),
            )
    }
}
