package io.github.fvory.psdk.ui

import io.github.fvory.psdk.config.PsdkConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PositionDialogTest {
    @Test
    fun parsesNumbersAndCommaDecimals() {
        assertEquals(50.0, parseCoordinate("50")!!, 0.0)
        assertEquals(12.5, parseCoordinate(" 12,5 ")!!, 0.0)
        assertEquals(0.0, parseCoordinate("0")!!, 0.0)
    }

    @Test
    fun rejectsBlankNegativeAndNonNumbers() {
        assertNull(parseCoordinate(""))
        assertNull(parseCoordinate("  "))
        assertNull(parseCoordinate("-1"))
        assertNull(parseCoordinate("abc"))
        assertNull(parseCoordinate("NaN"))
        assertNull(parseCoordinate("Infinity"))
    }

    @Test
    fun askPositionIsOffByDefault() {
        assertFalse(PsdkConfig().askPositionOnAdd)
    }
}
