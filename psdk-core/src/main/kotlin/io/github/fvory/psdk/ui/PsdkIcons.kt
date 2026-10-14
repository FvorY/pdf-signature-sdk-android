package io.github.fvory.psdk.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The web SDK's toolbar icons (Feather Icons, MIT license — see NOTICE), as 24-unit stroked vectors.
 * Tint them with `Icon(tint = …)`.
 */
internal object PsdkIcons {
    val upload by lazy {
        icon("upload", "M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4", "M17 8L12 3L7 8", "M12 3V15")
    }
    val signature by lazy {
        icon("signature", "M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2", circle(12f, 7f, 4f), "M12 14V21")
    }
    val estamp by lazy { icon("estamp", circle(12f, 12f, 9f), "M9 12l2 2 4-4") }
    val user by lazy { icon("user", "M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2", circle(12f, 7f, 4f)) }
    val prevPage by lazy { icon("prev", "M15 18L9 12L15 6") }
    val nextPage by lazy { icon("next", "M9 18L15 12L9 6") }
    val zoomIn by lazy { icon("zoom-in", circle(11f, 11f, 8f), "M21 21L16.65 16.65", "M11 8V14", "M8 11H14") }
    val zoomOut by lazy { icon("zoom-out", circle(11f, 11f, 8f), "M21 21L16.65 16.65", "M8 11H14") }
    val fit by lazy { icon("fit", "M15 3H21V9", "M9 21H3V15", "M21 3L14 10", "M3 21L10 14") }
    val clear by lazy { icon("clear", "M3 6H5H21", "M19 6l-1 14H6L5 6", "M10 11v6", "M14 11v6") }
    val moon by lazy { icon("moon", "M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z") }
    val sun by lazy {
        icon(
            "sun",
            circle(12f, 12f, 5f),
            "M12 1V3",
            "M12 21V23",
            "M4.22 4.22L5.64 5.64",
            "M18.36 18.36L19.78 19.78",
            "M1 12H3",
            "M21 12H23",
            "M4.22 19.78L5.64 18.36",
            "M18.36 5.64L19.78 4.22",
        )
    }
    val lock by lazy { icon("lock", "M5 11h14a2 2 0 012 2v7a2 2 0 01-2 2H5a2 2 0 01-2-2v-7a2 2 0 012-2z", "M7 11V7a5 5 0 0110 0v4") }
    val more by lazy { icon("more", circle(12f, 5f, 1f), circle(12f, 12f, 1f), circle(12f, 19f, 1f)) }
    val close by lazy { icon("close", "M18 6L6 18", "M6 6L18 18") }
    val eye by lazy { icon("eye", "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z", circle(12f, 12f, 3f)) }
    val eyeOff by lazy {
        icon(
            "eye-off",
            "M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94",
            "M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19",
            "M14.12 14.12a3 3 0 11-4.24-4.24",
            "M1 1L23 23",
        )
    }

    private fun circle(
        cx: Float,
        cy: Float,
        r: Float,
    ): String = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun icon(
        name: String,
        vararg paths: String,
    ): ImageVector =
        ImageVector
            .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                for (path in paths) {
                    addPath(
                        pathData = addPathNodes(path),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()
}
