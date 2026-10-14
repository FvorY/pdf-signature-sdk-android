package io.github.fvory.psdk.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.fvory.psdk.model.PsdkImage

/** Which parts of the viewer UI are shown (web: `ui`). */
public data class PsdkUiConfig(
    public val topbar: TopBarConfig = TopBarConfig(),
    public val sidebar: SidebarConfig = SidebarConfig(),
    public val viewer: ViewerUiConfig = ViewerUiConfig(),
    /** Status line with "Ready", the item count and the zoom. Always shown on the web. */
    public val statusbar: Boolean = true,
    /** Built-in password dialog. Turn off to show your own from the `passwordRequested` event. */
    public val passwordDialog: Boolean = true,
    /** Phone or tablet layout; [PsdkLayout.AUTO] switches at 600 dp width. */
    public val layout: PsdkLayout = PsdkLayout.AUTO,
    /** Short vibration when an item starts moving or is deleted. */
    public val haptics: Boolean = true,
)

/**
 * Toolbar contents (web: `ui.topbar`). On phones the buttons are spread over a top bar, a bottom bar,
 * a floating page indicator and a ⋮ menu; a button turned off here is hidden everywhere.
 */
public data class TopBarConfig(
    public val upload: Boolean = true,
    public val signature: Boolean = true,
    public val eStamp: Boolean = true,
    public val pagination: Boolean = true,
    public val paginationInput: Boolean = true,
    public val zoom: Boolean = true,
    public val themeToggle: Boolean = true,
    /** Extra content at the end of the top bar (web: `customComponent`). */
    public val customComponent: (@Composable () -> Unit)? = null,
    /** Logo before the brand text. No logo by default. */
    public val logo: PsdkImage? = null,
    /** Brand text. `null` shows `labels.brandDefault`; an empty string hides it. */
    public val brandText: String? = null,
    /** Replaces the web SDK's inline `style` object. */
    public val style: TopBarStyle? = null,
)

public data class TopBarStyle(
    public val background: Color? = null,
    public val content: Color? = null,
    public val height: Dp? = null,
    public val horizontalPadding: Dp? = null,
)

public data class SidebarConfig(
    /** Page thumbnails: a strip above the bottom bar on phones, a left sidebar on tablets. */
    public val thumbnails: Boolean = true,
)

public data class ViewerUiConfig(
    /** Pinch-to-zoom and double-tap zoom (web: `ui.viewer.zoom`). */
    public val zoom: Boolean = true,
)

public enum class PsdkLayout {
    AUTO,
    PHONE,
    TABLET,
}
