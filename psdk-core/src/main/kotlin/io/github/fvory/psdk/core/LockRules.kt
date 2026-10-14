package io.github.fvory.psdk.core

import io.github.fvory.psdk.model.SignatureItem

/** Viewer-wide lock options from the config. */
internal data class GlobalLocks(
    val disabled: Boolean = false,
    val disableDragging: Boolean = false,
    val disableDrag: Boolean = false,
    val disableResize: Boolean = false,
)

/** Lock matrix copied from the web SDK (B-LCK-01). */
internal object LockRules {
    fun canDrag(
        global: GlobalLocks,
        item: SignatureItem,
    ): Boolean = !(global.disableDragging || global.disableDrag || item.disableDragging || item.disableDrag || item.locked)

    fun canResize(
        global: GlobalLocks,
        item: SignatureItem,
    ): Boolean = !(global.disableResize || item.disableResize || global.disableDragging || item.disableDragging || item.locked)

    /** The delete button works even on locked items; only `disabled` turns it off. */
    fun canDelete(global: GlobalLocks): Boolean = !global.disabled
}
