package io.github.fvory.psdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Web SDK button styles: `psdk-btn--accent`, `--danger`, `--ghost`. */
internal enum class ButtonKind { ACCENT, DANGER, GHOST }

/**
 * Toolbar button with an icon and an optional label. Always at least 48 dp tall for touch
 * (B-A11Y-03); the label is also its accessibility description.
 */
@Composable
internal fun ToolbarButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    kind: ButtonKind = ButtonKind.GHOST,
    enabled: Boolean = true,
    contentColor: Color? = null,
) {
    val colors = LocalPsdkColors.current
    val (baseBackground, baseForeground) =
        when (kind) {
            ButtonKind.ACCENT -> colors.accent to colors.onAccent
            ButtonKind.DANGER -> colors.danger to Color.White
            ButtonKind.GHOST -> Color.Transparent to (contentColor ?: colors.textSecondary)
        }
    // Disabled = half-transparent colors. Not Modifier.alpha: switching it on and off changes the
    // modifier chain, and on some devices the background then stopped drawing after a document load.
    val dim = if (enabled) 1f else 0.5f
    val background = baseBackground.copy(alpha = baseBackground.alpha * dim)
    val foreground = baseForeground.copy(alpha = baseForeground.alpha * dim)
    Row(
        modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = if (label == null) 8.dp else 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
        if (label != null) {
            Text(
                label,
                color = foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
