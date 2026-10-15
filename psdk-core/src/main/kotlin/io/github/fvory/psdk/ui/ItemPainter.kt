package io.github.fvory.psdk.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.config.PsdkColors
import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.SignatureItem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Draws one item the way the web SDK does (B-DRW-01…07). Handle sizes are fixed in dp. */
internal object ItemPainter {
    private val DELETE_RADIUS = 10.dp
    private val RESIZE_RADIUS = 8.dp
    private val CORNER = 6.dp

    fun DrawScope.drawItem(
        item: SignatureItem,
        image: ImageBitmap?,
        scale: Float,
        canDrag: Boolean,
        canResize: Boolean,
        active: Boolean,
        textMeasurer: TextMeasurer,
        colors: PsdkColors,
    ) {
        val pxPerPoint = scale * density
        val topLeft = Offset((item.x * pxPerPoint).toFloat(), (item.y * pxPerPoint).toFloat())
        val size = Size((item.width * pxPerPoint).toFloat(), (item.height * pxPerPoint).toFloat())
        val isStamp = item.type == ItemType.ESTAMP

        if (active) {
            drawRoundRect(Color.Black.copy(alpha = 0.12f), topLeft + Offset(0f, 3.dp.toPx()), size, CornerRadius(CORNER.toPx()))
        }

        when {
            image != null ->
                drawImage(
                    image = image,
                    dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                    dstSize = IntSize(max(1, size.width.roundToInt()), max(1, size.height.roundToInt())),
                )
            isStamp -> drawStampPlaceholder(item, topLeft, size, active, textMeasurer, colors)
            else -> drawSignaturePlaceholder(item, topLeft, size, active, textMeasurer, colors)
        }

        if (canDrag || canResize) {
            val border = Stroke(width = 1.dp.toPx())
            if (isStamp) {
                drawRect(Color.White.copy(alpha = 0.4f), topLeft, size, style = border)
            } else {
                drawRoundRect(Color.White.copy(alpha = 0.4f), topLeft, size, CornerRadius(CORNER.toPx()), style = border)
            }
        }

        drawDeleteHandle(Offset(topLeft.x + size.width, topLeft.y), colors)
        if (canResize) drawResizeHandle(Offset(topLeft.x + size.width, topLeft.y + size.height))
    }

    private fun DrawScope.drawSignaturePlaceholder(
        item: SignatureItem,
        topLeft: Offset,
        size: Size,
        active: Boolean,
        textMeasurer: TextMeasurer,
        colors: PsdkColors,
    ) {
        val corner = CornerRadius(CORNER.toPx())
        if (!active) {
            val glow = 4.dp.toPx()
            drawRoundRect(
                colors.accent.copy(alpha = 0.18f),
                topLeft - Offset(glow, glow),
                Size(size.width + glow * 2, size.height + glow * 2),
                CornerRadius(corner.x + glow),
            )
        }
        drawRoundRect(colors.accent.copy(alpha = 0.12f), topLeft, size, corner)
        drawRoundRect(
            colors.accent.copy(alpha = 0.9f),
            topLeft,
            size,
            corner,
            style =
                Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())),
                ),
        )
        drawLabel(item.label, topLeft, size, colors.accent, textMeasurer)
    }

    private fun DrawScope.drawStampPlaceholder(
        item: SignatureItem,
        topLeft: Offset,
        size: Size,
        active: Boolean,
        textMeasurer: TextMeasurer,
        colors: PsdkColors,
    ) {
        val center = Offset(topLeft.x + size.width / 2, topLeft.y + size.height / 2)
        val radius = max(1f, min(size.width, size.height) / 2 - 4.dp.toPx())
        if (!active) drawCircle(colors.danger.copy(alpha = 0.15f), radius + 4.dp.toPx(), center)
        drawCircle(colors.danger.copy(alpha = 0.08f), radius, center)
        drawCircle(colors.danger.copy(alpha = 0.9f), radius, center, style = Stroke(2.dp.toPx()))
        drawLabel(item.label, topLeft, size, colors.danger, textMeasurer)
    }

    private fun DrawScope.drawLabel(
        text: String,
        topLeft: Offset,
        size: Size,
        color: Color,
        textMeasurer: TextMeasurer,
    ) {
        val maxWidth = (size.width - 8.dp.toPx()).roundToInt()
        if (maxWidth <= 0) return
        val layout =
            textMeasurer.measure(
                text = text,
                style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = maxWidth),
            )
        drawText(
            layout,
            topLeft = Offset(topLeft.x + (size.width - layout.size.width) / 2, topLeft.y + (size.height - layout.size.height) / 2),
        )
    }

    private fun DrawScope.drawDeleteHandle(
        center: Offset,
        colors: PsdkColors,
    ) {
        drawCircle(colors.danger.copy(alpha = 0.85f), DELETE_RADIUS.toPx(), center)
        val arm = 3.5.dp.toPx()
        val stroke = 1.5.dp.toPx()
        drawLine(Color.White, center + Offset(-arm, -arm), center + Offset(arm, arm), stroke, StrokeCap.Round)
        drawLine(Color.White, center + Offset(arm, -arm), center + Offset(-arm, arm), stroke, StrokeCap.Round)
    }

    private fun DrawScope.drawResizeHandle(center: Offset) {
        drawCircle(Color.White, RESIZE_RADIUS.toPx(), center)
        drawCircle(Color(0xFF666666), RESIZE_RADIUS.toPx(), center, style = Stroke(1.dp.toPx()))
        val arm = 2.5.dp.toPx()
        drawLine(Color(0xFF333333), center + Offset(-arm, arm), center + Offset(arm, -arm), 1.dp.toPx(), StrokeCap.Round)
    }
}
