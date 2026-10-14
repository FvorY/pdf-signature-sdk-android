package io.github.fvory.psdk.core

import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.SignatureItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-U09 — every combination of the 8 lock flags against the web SDK expressions (B-LCK-01). */
class LockRulesTest {
    @Test
    fun allCombinationsMatchWebFormula() {
        for (bits in 0 until 256) {
            fun flag(index: Int) = bits and (1 shl index) != 0
            val global = GlobalLocks(disableDragging = flag(0), disableDrag = flag(1), disableResize = flag(2))
            val item =
                SignatureItem(
                    id = "i",
                    type = ItemType.SIGNATURE,
                    x = 0.0,
                    y = 0.0,
                    page = 1,
                    label = "",
                    width = 1.0,
                    height = 1.0,
                    disableDragging = flag(3),
                    disableDrag = flag(4),
                    disableResize = flag(5),
                    locked = flag(6),
                )
            // Copied from the web SDK's _hitTest / _drawSignature:
            val webCanDrag = !(flag(0) || flag(1) || flag(3) || flag(4) || flag(6))
            val webCanResize = !(flag(2) || flag(5) || flag(0) || flag(3) || flag(6))

            assertEquals("drag, bits=$bits", webCanDrag, LockRules.canDrag(global, item))
            assertEquals("resize, bits=$bits", webCanResize, LockRules.canResize(global, item))
        }
    }

    @Test
    fun deleteOnlyBlockedByDisabled() {
        assertTrue(LockRules.canDelete(GlobalLocks(disableDragging = true, disableDrag = true, disableResize = true)))
        assertFalse(LockRules.canDelete(GlobalLocks(disabled = true)))
    }
}
