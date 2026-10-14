package io.github.fvory.psdk.ui

import io.github.fvory.psdk.config.PickerOption
import io.github.fvory.psdk.config.PsdkLabels
import io.github.fvory.psdk.config.PsdkLayout
import io.github.fvory.psdk.config.TopBarConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-U21 and toolbar rules: B-INIT-04, B-TB-03, B-MOD-03/04, B-ST-01, B-PG-08. */
class ChromeRulesTest {
    private val labels = PsdkLabels()
    private val allOff =
        TopBarConfig(
            upload = false,
            signature = false,
            eStamp = false,
            pagination = false,
            paginationInput = false,
            zoom = false,
            themeToggle = false,
        )

    @Test
    fun topBarHiddenOnlyWhenEverythingIsOff() {
        assertTrue(ChromeRules.topBarHasContent(TopBarConfig()))
        assertFalse(ChromeRules.topBarHasContent(allOff))
        assertTrue(ChromeRules.topBarHasContent(allOff.copy(zoom = true)))
        assertTrue(ChromeRules.topBarHasContent(allOff.copy(brandText = "ezSign")))
        assertFalse(ChromeRules.topBarHasContent(allOff.copy(brandText = "")))
    }

    @Test
    fun layoutSwitchesAt600dp() {
        assertFalse(ChromeRules.isTablet(PsdkLayout.AUTO, 599f))
        assertTrue(ChromeRules.isTablet(PsdkLayout.AUTO, 600f))
        assertTrue(ChromeRules.isTablet(PsdkLayout.TABLET, 320f))
        assertFalse(ChromeRules.isTablet(PsdkLayout.PHONE, 1200f))
    }

    @Test
    fun brandText() {
        assertEquals("PDF Viewer", ChromeRules.brandText(TopBarConfig(), labels))
        assertNull(ChromeRules.brandText(TopBarConfig(brandText = ""), labels))
        assertEquals("ezSign", ChromeRules.brandText(TopBarConfig(brandText = "ezSign"), labels))
    }

    @Test
    fun groupsKeepFirstAppearanceOrderAndUseDefaultGroup() {
        val options =
            listOf(
                PickerOption("a", group = "Eksternal"),
                PickerOption("b"),
                PickerOption("c", group = "Internal"),
                PickerOption("d", group = "Eksternal"),
                PickerOption("e", group = ""),
            )
        val groups = ChromeRules.group(options, "Umum")
        assertEquals(listOf("Eksternal", "Umum", "Internal"), groups.map { it.first })
        assertEquals(listOf("a", "d"), groups[0].second.map { it.id })
        assertEquals(listOf("b", "e"), groups[1].second.map { it.id })
    }

    @Test
    fun optionLabelFallsBack() {
        assertEquals("Direktur", ChromeRules.optionLabel(PickerOption("x", "Direktur"), true, labels))
        assertEquals("Signature", ChromeRules.optionLabel(PickerOption("x", ""), true, labels))
        assertEquals("E-Materai", ChromeRules.optionLabel(PickerOption("x"), false, labels))
    }

    @Test
    fun itemCountText() {
        assertNull(ChromeRules.itemCount(0, labels))
        assertEquals("1 signature", ChromeRules.itemCount(1, labels))
        assertEquals("3 signatures", ChromeRules.itemCount(3, labels))
        assertEquals("3 tanda tangan", ChromeRules.itemCount(3, PsdkLabels.indonesian()))
    }

    @Test
    fun pageInputParsing() {
        assertEquals(12, ChromeRules.parsePage(" 12 "))
        assertNull(ChromeRules.parsePage("abc"))
        assertNull(ChromeRules.parsePage(""))
    }
}
