package io.github.fvory.psdk.config

import io.github.fvory.psdk.model.PdfSource

/**
 * Viewer configuration. Mirrors the web SDK `createViewer(config)` options; options that only
 * make sense on the web (`container`, CSS) are replaced by native equivalents.
 *
 * More options (signature/E-Materai pickers, locks, theme, toolbar) are added in milestones M2–M3.
 */
public data class PsdkConfig(
    /** Loaded automatically when the viewer is created. Errors are logged, not thrown (B-INIT-02). */
    public val file: PdfSource? = null,
    /**
     * Initial zoom, 0.25–4.0. `null` (default) fits the widest page to the screen width.
     * The web SDK defaults to 1.5; coordinates do not depend on the zoom.
     */
    public val scale: Float? = null,
    /** Turns off every button, the thumbnails and touching items. API methods keep working (B-LCK-05). */
    public val disabled: Boolean = false,
    /** Locks moving and resizing of all items. The delete button stays active. */
    public val disableDragging: Boolean = false,
    /** Locks moving of all items; resizing still works. Prefer [disableDragging] (the web SDK marks this deprecated). */
    public val disableDrag: Boolean = false,
    /** Hides the resize handle of all items; moving still works. */
    public val disableResize: Boolean = false,
    /**
     * Keep an E-Materai's aspect ratio while resizing. Off by default: like the web SDK, width and
     * height change independently and the height follows the image when it loads.
     */
    public val estampKeepAspectRatio: Boolean = false,
    /**
     * When `setPaginationLocked(true)` is active, also stop the user from scrolling to other pages.
     * Off by default, like the web SDK, where only the buttons, page input and thumbnails are locked.
     */
    public val lockScrollWhenPaginationLocked: Boolean = false,
    public val labels: PsdkLabels = PsdkLabels(),
    public val ui: PsdkUiConfig = PsdkUiConfig(),
    public val logLevel: PsdkLogLevel = PsdkLogLevel.ERROR,
)

public data class PsdkUiConfig(
    public val viewer: ViewerUiConfig = ViewerUiConfig(),
    /** Short vibration when an item starts moving or is deleted. */
    public val haptics: Boolean = true,
)

public data class ViewerUiConfig(
    /** Pinch-to-zoom and double-tap zoom (web: `ui.viewer.zoom`). */
    public val zoom: Boolean = true,
)

public enum class PsdkLogLevel {
    NONE,
    ERROR,
    DEBUG,
}
