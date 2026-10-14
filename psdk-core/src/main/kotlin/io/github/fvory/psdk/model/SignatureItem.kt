package io.github.fvory.psdk.model

import org.json.JSONArray
import org.json.JSONObject

/** Kind of placed item. [value] is the string used by the web SDK. */
public enum class ItemType(
    public val value: String,
) {
    SIGNATURE("signature"),
    ESTAMP("estamp"),
}

/**
 * A placed signature or E-Materai, exactly as the web SDK reports it (B-OVL-08).
 *
 * Positions are in PDF points with the origin at the page's top-left corner and y pointing down.
 * [x], [y] are the item's top-left corner.
 */
public data class SignatureItem(
    public val id: String,
    public val type: ItemType,
    public val x: Double,
    public val y: Double,
    public val page: Int,
    public val label: String,
    public val width: Double,
    public val height: Double,
    public val disableDrag: Boolean = false,
    public val disableDragging: Boolean = false,
    public val disableResize: Boolean = false,
    public val locked: Boolean = false,
) {
    /** Field names and order of the web SDK's public item. */
    public fun toMap(): Map<String, Any> =
        linkedMapOf(
            "id" to id,
            "type" to type.value,
            "x" to x,
            "y" to y,
            "page" to page,
            "label" to label,
            "width" to width,
            "height" to height,
            "disableDrag" to disableDrag,
            "disableDragging" to disableDragging,
            "disableResize" to disableResize,
            "locked" to locked,
        )

    /** JSON text identical to `JSON.stringify(item)` in the web SDK (whole numbers have no ".0"). */
    public fun toJsonString(): String = JsonText.obj(toMap())

    public fun toJson(): JSONObject = JSONObject(toJsonString())

    /**
     * The item's bottom edge measured from the page bottom, for PDF libraries whose origin is
     * the bottom-left corner (iText, PDFBox). Spec B-CRD-05.
     */
    public fun yFromBottom(pageHeight: Double): Double = pageHeight - y - height
}

/** `JSON.stringify(viewer.getSignatures())` equivalent. */
public fun List<SignatureItem>.toJsonString(): String = joinToString(",", "[", "]") { it.toJsonString() }

public fun List<SignatureItem>.toJsonArray(): JSONArray = JSONArray(toJsonString())

/** Minimal JSON writer with JavaScript number formatting, so output matches the web SDK byte for byte. */
internal object JsonText {
    fun obj(map: Map<String, Any>): String = map.entries.joinToString(",", "{", "}") { (key, value) -> "${string(key)}:${value(value)}" }

    private fun value(value: Any): String =
        when (value) {
            is String -> string(value)
            is Boolean -> value.toString()
            is Int, is Long -> value.toString()
            is Double -> number(value)
            is Float -> number(value.toDouble())
            else -> string(value.toString())
        }

    fun number(value: Double): String =
        when {
            value.isNaN() || value.isInfinite() -> "null"
            value == Math.rint(value) && kotlin.math.abs(value) < 1e15 -> value.toLong().toString()
            else -> value.toString()
        }

    fun string(value: String): String {
        val out = StringBuilder("\"")
        for (char in value) {
            when (char) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> if (char < ' ') out.append("\\u%04x".format(char.code)) else out.append(char)
            }
        }
        return out.append('"').toString()
    }
}
