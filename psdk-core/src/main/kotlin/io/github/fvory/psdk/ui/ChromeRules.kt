package io.github.fvory.psdk.ui

import io.github.fvory.psdk.config.PickerOption
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.config.PsdkLayout
import io.github.fvory.psdk.config.TopBarConfig
import kotlin.math.roundToInt

/** Pure rules behind the toolbar, picker and status line, kept apart so they can be unit tested. */
internal object ChromeRules {
    const val TABLET_MIN_WIDTH_DP: Float = 600f
    const val ICON_ONLY_MAX_WIDTH_DP: Float = 360f

    /** Below this viewer height the phone thumbnail strip is hidden, so the bottom bar keeps its room. */
    const val MIN_HEIGHT_FOR_THUMBNAILS_DP: Float = 560f

    /** Below this viewer height the status line is hidden as well. */
    const val MIN_HEIGHT_FOR_STATUSBAR_DP: Float = 420f

    /**
     * What fits in a short viewer. The bottom bar (signature, E-Materai) has priority: the thumbnail
     * strip goes first, then the status line, instead of the bottom bar being cut off.
     */
    fun showThumbnailStrip(heightDp: Float): Boolean = heightDp >= MIN_HEIGHT_FOR_THUMBNAILS_DP

    fun showStatusBar(heightDp: Float): Boolean = heightDp >= MIN_HEIGHT_FOR_STATUSBAR_DP

    /** B-INIT-04: no top bar when every toolbar option is off and there is nothing custom to show. */
    fun topBarHasContent(topbar: TopBarConfig): Boolean =
        topbar.upload ||
            topbar.signature ||
            topbar.eStamp ||
            topbar.pagination ||
            topbar.paginationInput ||
            topbar.zoom ||
            topbar.themeToggle ||
            topbar.customComponent != null ||
            topbar.logo != null ||
            !topbar.brandText.isNullOrEmpty()

    fun isTablet(
        layout: PsdkLayout,
        widthDp: Float,
    ): Boolean =
        when (layout) {
            PsdkLayout.PHONE -> false
            PsdkLayout.TABLET -> true
            PsdkLayout.AUTO -> widthDp >= TABLET_MIN_WIDTH_DP
        }

    /** B-TB-03: `""` hides the brand, `null` uses the default text. */
    fun brandText(
        topbar: TopBarConfig,
        labels: PsdkLabels,
    ): String? =
        when (val text = topbar.brandText) {
            null -> labels.brandDefault
            "" -> null
            else -> text
        }

    /** B-MOD-03: groups in order of first appearance; options without a group go under [defaultGroup]. */
    fun group(
        options: List<PickerOption>,
        defaultGroup: String,
    ): List<Pair<String, List<PickerOption>>> =
        options
            .groupBy { it.group.orEmpty() }
            .map { (group, items) -> (group.ifEmpty { defaultGroup }) to items }

    /** B-MOD-04: option text, or the default label when it has none. */
    fun optionLabel(
        option: PickerOption,
        isSignature: Boolean,
        labels: PsdkLabels,
    ): String = option.label?.takeIf { it.isNotEmpty() } ?: if (isSignature) labels.defaultSignatureLabel else labels.defaultEStampLabel

    /** B-ST-01: "1 signature" / "N signatures", null when there are none. */
    fun itemCount(
        count: Int,
        labels: PsdkLabels,
    ): String? =
        when (count) {
            0 -> null
            1 -> labels.signatureCountOne.format(count)
            else -> labels.signatureCountOther.format(count)
        }

    fun zoomPercent(scale: Float): String = "${(scale * 100).roundToInt()}%"

    /** B-PG-08: parsed page number, or null when the text is not a number. */
    fun parsePage(text: String): Int? = text.trim().toIntOrNull()
}
