package io.github.fvory.psdk.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Images drawn in code so the sample needs no network or assets. */
internal object SampleImages {
    /** A blue handwritten-looking stroke on a transparent 400 × 120 bitmap (ratio 10:3). */
    val signature: Bitmap by lazy {
        Bitmap.createBitmap(400, 120, Bitmap.Config.ARGB_8888).also { bitmap ->
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(29, 78, 216)
                    style = Paint.Style.STROKE
                    strokeWidth = 7f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            val path =
                Path().apply {
                    moveTo(20f, 85f)
                    cubicTo(60f, 10f, 90f, 10f, 100f, 70f)
                    cubicTo(110f, 120f, 150f, 20f, 180f, 60f)
                    cubicTo(200f, 90f, 230f, 30f, 260f, 65f)
                    cubicTo(290f, 100f, 330f, 40f, 380f, 50f)
                }
            Canvas(bitmap).drawPath(path, paint)
        }
    }

    /** A 120 × 140 stamp, deliberately not square, to show that E-Materai follows the image ratio. */
    val stamp: Bitmap by lazy {
        Bitmap.createBitmap(120, 140, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.rgb(254, 226, 226)
            canvas.drawRoundRect(RectF(2f, 2f, 118f, 138f), 10f, 10f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.rgb(220, 38, 38)
            canvas.drawRoundRect(RectF(2f, 2f, 118f, 138f), 10f, 10f, paint)
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true
            paint.textSize = 18f
            canvas.drawText("METERAI", 60f, 60f, paint)
            paint.textSize = 22f
            canvas.drawText("10000", 60f, 92f, paint)
        }
    }
}
