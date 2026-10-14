package io.github.fvory.psdk.core

import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.SignatureItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** TC-U10 — B-INT-01/02, spec §14.1. */
class HitTesterTest {
    // At scale 1: item spans x 100..260 dp, y 200..248 dp
    private fun item(
        id: String,
        locked: Boolean = false,
        disableResize: Boolean = false,
    ) = SignatureItem(id, ItemType.SIGNATURE, 100.0, 200.0, 1, "", 160.0, 48.0, locked = locked, disableResize = disableResize)

    private val free = GlobalLocks()

    @Test
    fun deleteZoneIsCircleAroundTopRight() {
        assertEquals(HitPart.DELETE, HitTester.hit(listOf(item("a")), 260f, 200f, 1f, free)?.part)
        assertEquals(HitPart.DELETE, HitTester.hit(listOf(item("a")), 276f, 216f, 1f, free)?.part)
        // 30 dp away diagonally is outside the 24 dp radius and outside the box
        assertNull(HitTester.hit(listOf(item("a")), 281f, 179f, 1f, free))
    }

    @Test
    fun resizeZoneIsSquareAroundBottomRight() {
        assertEquals(HitPart.RESIZE, HitTester.hit(listOf(item("a")), 280f, 270f, 1f, free)?.part)
        assertEquals(HitPart.BODY, HitTester.hit(listOf(item("a", disableResize = true)), 255f, 245f, 1f, free)?.part)
    }

    @Test
    fun bodyInsideBox() {
        assertEquals(HitPart.BODY, HitTester.hit(listOf(item("a")), 150f, 220f, 1f, free)?.part)
        assertNull(HitTester.hit(listOf(item("a")), 50f, 220f, 1f, free))
    }

    @Test
    fun lockedItemBodyIsNotHitButDeleteStillIs() {
        val locked = listOf(item("a", locked = true))
        assertNull(HitTester.hit(locked, 150f, 220f, 1f, free))
        assertEquals(HitPart.DELETE, HitTester.hit(locked, 260f, 200f, 1f, free)?.part)
    }

    @Test
    fun topmostItemWins() {
        val items = listOf(item("bottom"), item("top"))
        assertEquals("top", HitTester.hit(items, 150f, 220f, 1f, free)?.item?.id)
    }

    @Test
    fun scaleMovesTheBoxButNotTheTouchAreas() {
        // At scale 2 the box spans x 200..520, y 400..496 dp
        assertEquals(HitPart.BODY, HitTester.hit(listOf(item("a")), 300f, 450f, 2f, free)?.part)
        assertEquals(HitPart.DELETE, HitTester.hit(listOf(item("a")), 540f, 400f, 2f, free)?.part)
    }

    @Test
    fun disabledHitsNothing() {
        assertNull(HitTester.hit(listOf(item("a")), 260f, 200f, 1f, GlobalLocks(disabled = true)))
    }
}
