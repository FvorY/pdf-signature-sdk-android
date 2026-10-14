package io.github.fvory.psdk.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import io.github.fvory.psdk.model.PdfSource
import io.github.fvory.psdk.model.PsdkImage

/**
 * A choice in the signature or E-Materai picker (web: `signatureOptions` / `estampOptions` entries).
 * Choosing it places an item with this [label] and [image] on the current page.
 */
public data class PickerOption(
    public val id: String,
    public val label: String? = null,
    public val image: PsdkImage? = null,
    /** Heading the option is listed under when `groupByCategory` is on. */
    public val group: String? = null,
)

/** Result of `onUpload` (web: return `false`, a replacement file, or anything else). */
public sealed interface UploadDecision {
    /** Open the file the user picked. */
    public data object Proceed : UploadDecision

    /** Do not open anything. */
    public data object Cancel : UploadDecision

    /** Open [source] instead of the picked file. */
    public data class Replace(
        public val source: PdfSource,
    ) : UploadDecision
}

/** Options of `showSkeleton` (web: `{ opacity, blur, customHTML }`). */
public data class SkeletonOptions(
    /** 0–1, opacity of the background behind the spinner. */
    public val backgroundOpacity: Float? = null,
    /** Blur of the pages behind the overlay. Android 12+ only; older versions just dim. */
    public val blurRadius: Dp? = null,
    /** Replaces the default spinner. */
    public val content: (@Composable () -> Unit)? = null,
)
