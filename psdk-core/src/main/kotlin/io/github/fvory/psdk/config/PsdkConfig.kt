package io.github.fvory.psdk.config

import androidx.compose.ui.text.font.FontFamily
import io.github.fvory.psdk.model.PdfSource

/**
 * Viewer configuration. Mirrors the web SDK `createViewer(config)` options; options that only
 * make sense on the web (`container`, CSS) are replaced by native equivalents.
 * Change it at runtime with `PdfSignatureViewer.updateConfig { it.copy(...) }`.
 */
public data class PsdkConfig(
    /** Loaded automatically when the viewer is created. Errors are logged, not thrown (B-INIT-02). */
    public val file: PdfSource? = null,
    /**
     * Initial zoom, 0.25–4.0. `null` (default) fits the widest page to the screen width.
     * The web SDK defaults to 1.5; coordinates do not depend on the zoom.
     */
    public val scale: Float? = null,
    public val theme: PsdkThemeMode = PsdkThemeMode.LIGHT,
    /** Colors used by the light theme. Customize with `PsdkColors.light().copy(accent = …)`. */
    public val lightColors: PsdkColors = PsdkColors.light(),
    /** Colors used by the dark theme. */
    public val darkColors: PsdkColors = PsdkColors.dark(),
    /** Font of the viewer UI (web: `--psdk-font`). `null` uses the system font. */
    public val fontFamily: FontFamily? = null,
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
    /** Choices in the "Add Signature" picker. Empty: the button places a default signature directly. */
    public val signatureOptions: List<PickerOption> = emptyList(),
    /** Choices in the E-Materai picker. Empty: the button places a default E-Materai directly. */
    public val estampOptions: List<PickerOption> = emptyList(),
    /** List picker options under their `group` headings. */
    public val groupByCategory: Boolean = false,
    /**
     * Called after the user picks a file with the upload button, before it is opened.
     * Return [UploadDecision.Cancel], [UploadDecision.Replace] or [UploadDecision.Proceed].
     */
    public val onUpload: (suspend (PdfSource) -> UploadDecision)? = null,
    /**
     * When `setPaginationLocked(true)` is active, also stop the user from scrolling to other pages.
     * Off by default, like the web SDK, where only the buttons, page input and thumbnails are locked.
     */
    public val lockScrollWhenPaginationLocked: Boolean = false,
    public val labels: PsdkLabels = PsdkLabels(),
    public val ui: PsdkUiConfig = PsdkUiConfig(),
    public val logLevel: PsdkLogLevel = PsdkLogLevel.ERROR,
)

public enum class PsdkLogLevel {
    NONE,
    ERROR,
    DEBUG,
}
