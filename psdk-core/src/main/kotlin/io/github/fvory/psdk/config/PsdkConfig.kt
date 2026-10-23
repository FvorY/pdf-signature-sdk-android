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
     * Allows remote HTTP sources. HTTPS is required by default; enable this only for a trusted
     * legacy endpoint that cannot serve TLS.
     */
    public val allowInsecureHttp: Boolean = false,
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
    /**
     * Block screenshots, screen recording and the recent-apps preview while the viewer is shown, by
     * setting `FLAG_SECURE` on the window. The SDK's dialogs and sheets inherit it. The flag is only
     * removed again if the SDK added it.
     */
    public val secureScreen: Boolean = false,
    public val labels: PsdkLabels = PsdkLabels(),
    public val ui: PsdkUiConfig = PsdkUiConfig(),
    public val logLevel: PsdkLogLevel = PsdkLogLevel.ERROR,
    /**
     * Where the "Add Signature" / E-Materai buttons and pickers place a new item, so the user does not
     * have to drag it. `null` (default) centers it on the page. Origin is the top-left corner of the
     * page in PDF points, the same as [io.github.fvory.psdk.model.PlaceOptions.x]. The item is kept
     * inside the page. `placeSignature` / `placeEStamp` are not affected: they use their own options.
     */
    public val defaultPosition: PlacementPosition? = null,
    /**
     * Ask for the position before an item from the "Add Signature" / E-Materai buttons is placed: the
     * built-in dialog takes X and Y (PDF points, origin top-left). Both fields empty places the item
     * at [defaultPosition] or the page center; Cancel adds nothing. Off by default: the item is
     * placed right away. `placeSignature` / `placeEStamp` are not affected.
     */
    public val askPositionOnAdd: Boolean = false,
)

/** Top-left position of an item on its page, in PDF points (origin top-left, y grows downward). */
public data class PlacementPosition(
    public val x: Double,
    public val y: Double,
)

public enum class PsdkLogLevel {
    NONE,
    ERROR,
    DEBUG,
}
