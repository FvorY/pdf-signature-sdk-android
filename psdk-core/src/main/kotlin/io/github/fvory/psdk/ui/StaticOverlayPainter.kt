package io.github.fvory.psdk.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.SignatureItem
import kotlin.math.min

/** Draws items onto a rendered page bitmap for `renderPageWithOverlay` (no handles). */
internal object StaticOverlayPainter {
    fun draw(
        canvas: Canvas,
        items: List<SignatureItem>,
        bitmaps: Map<String, Bitmap>,
        pxPerPoint: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        for (item in items) {
            val rect =
                RectF(
                    (item.x * pxPerPoint).toFloat(),
                    (item.y * pxPerPoint).toFloat(),
                    ((item.x + item.width) * pxPerPoint).toFloat(),
                    ((item.y + item.height) * pxPerPoint).toFloat(),
                )
            val image = bitmaps[item.id]
            if (image != null) {
                canvas.drawBitmap(image, null, rect, paint)
                continue
            }
            val color = if (item.type == ItemType.ESTAMP) Palette.danger.toArgb() else Palette.accent.toArgb()
            paint.color = color
            paint.alpha = 31
            paint.style = Paint.Style.FILL
            paint.pathEffect = null
            if (item.type == ItemType.ESTAMP) {
                val radius = min(rect.width(), rect.height()) / 2
                canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint)
                paint.style = Paint.Style.STROKE
                paint.alpha = 230
                paint.strokeWidth = 2 * pxPerPoint
                canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint)
            } else {
                canvas.drawRoundRect(rect, 6 * pxPerPoint, 6 * pxPerPoint, paint)
                paint.style = Paint.Style.STROKE
                paint.alpha = 230
                paint.strokeWidth = 1.5f * pxPerPoint
                paint.pathEffect = DashPathEffect(floatArrayOf(5 * pxPerPoint, 3 * pxPerPoint), 0f)
                canvas.drawRoundRect(rect, 6 * pxPerPoint, 6 * pxPerPoint, paint)
            }
            paint.pathEffect = null
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 11 * pxPerPoint
            paint.isFakeBoldText = true
            canvas.drawText(item.label, rect.centerX(), rect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
            paint.isFakeBoldText = false
        }
    }
}
