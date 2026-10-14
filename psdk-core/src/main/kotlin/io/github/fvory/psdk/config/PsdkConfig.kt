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
    public val labels: PsdkLabels = PsdkLabels(),
    public val ui: PsdkUiConfig = PsdkUiConfig(),
    public val logLevel: PsdkLogLevel = PsdkLogLevel.ERROR,
)

public data class PsdkUiConfig(
    public val viewer: ViewerUiConfig = ViewerUiConfig(),
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
