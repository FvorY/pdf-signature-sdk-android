package io.github.fvory.psdk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fvory.psdk.config.PsdkLabels

/**
 * "No Document Loaded" screen (B-INIT-03), with an "Open PDF" button when uploading is possible
 * (spec §13.4). Shows the load error instead of the subtitle when there is one.
 */
@Composable
internal fun EmptyState(
    labels: PsdkLabels,
    error: Throwable?,
    onUpload: (() -> Unit)?,
) {
    val colors = LocalPsdkColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val muted = colors.textMuted
        Canvas(Modifier.size(48.dp)) {
            // The web SDK's empty-state icon (file with a plus sign) on a 24-unit grid
            val unit = size.minDimension / 24f
            val stroke = Stroke(width = 1.5f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val outline =
                Path().apply {
                    moveTo(14 * unit, 2 * unit)
                    lineTo(6 * unit, 2 * unit)
                    quadraticTo(4 * unit, 2 * unit, 4 * unit, 4 * unit)
                    lineTo(4 * unit, 20 * unit)
                    quadraticTo(4 * unit, 22 * unit, 6 * unit, 22 * unit)
                    lineTo(18 * unit, 22 * unit)
                    quadraticTo(20 * unit, 22 * unit, 20 * unit, 20 * unit)
                    lineTo(20 * unit, 8 * unit)
                    close()
                    moveTo(14 * unit, 2 * unit)
                    lineTo(14 * unit, 8 * unit)
                    lineTo(20 * unit, 8 * unit)
                }
            drawPath(outline, muted, style = stroke)
            drawLine(muted, Offset(12 * unit, 12 * unit), Offset(12 * unit, 18 * unit), stroke.width, StrokeCap.Round)
            drawLine(muted, Offset(9 * unit, 15 * unit), Offset(15 * unit, 15 * unit), stroke.width, StrokeCap.Round)
        }
        Text(
            text = if (error == null) labels.emptyTitle else labels.errorLoad,
            color = if (error == null) colors.textPrimary else colors.danger,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = error?.message ?: labels.emptySubtitle,
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (onUpload != null) {
            ToolbarButton(
                PsdkIcons.upload,
                labels.uploadBtn,
                onUpload,
                label = labels.uploadBtn,
                kind = ButtonKind.ACCENT,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
