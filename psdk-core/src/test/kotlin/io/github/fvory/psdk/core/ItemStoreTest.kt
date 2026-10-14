package io.github.fvory.psdk.core

import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.PsdkImage
import io.github.fvory.psdk.model.SignatureItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** TC-U02…U08 — B-OVL-01…11. */
class ItemStoreTest {
    private val a4 = PageSize(595.0, 842.0)
    private val events = mutableListOf<PsdkEvent>()
    private var lastList: List<SignatureItem> = emptyList()
    private val store =
        ItemStore(
            ids = IdGenerator(clock = { 1_760_000_000_000 }, random = Random(42)),
            onChanged = { lastList = it },
            emit = { events += it },
        )

    @Test
    fun idFormatMatchesWeb() {
        val generator = IdGenerator(clock = { 1_760_000_000_000 })
        val ids = (1..10_000).map { generator.next(ItemType.SIGNATURE) }
        assertTrue(ids.all { Regex("signature-1760000000000-[0-9a-z]{5}").matches(it) })
        assertTrue(ids.toSet().size > 9_990)
        assertTrue(Regex("estamp-\\d+-[0-9a-z]{5}").matches(IdGenerator().next(ItemType.ESTAMP)))
    }

    @Test
    fun signatureDefaultsAreCenteredWithWebSizes() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(), 1, a4)
        // Spec §5.4: x = 595/2 − 80, y = 842/2 − 24
        assertEquals(217.5, item.x, 0.0)
        assertEquals(397.0, item.y, 0.0)
        assertEquals(160.0, item.width, 0.0)
        assertEquals(48.0, item.height, 0.0)
        assertEquals("Signature", item.label)
    }

    @Test
    fun estampDefaultsAreCenteredSquareAndAlwaysLabelledEMaterai() {
        val item = store.place(ItemType.ESTAMP, PlaceOptions(label = "Ignored"), 1, a4)
        assertEquals(257.5, item.x, 0.0)
        assertEquals(381.0, item.y, 0.0)
        assertEquals(80.0, item.width, 0.0)
        assertEquals(80.0, item.height, 0.0)
        assertEquals("E-Materai", item.label)
    }

    @Test
    fun centeringUsesDefaultSizeEvenWhenSizeIsGiven() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(width = 200.0, height = 60.0), 1, a4)
        assertEquals(217.5, item.x, 0.0)
        assertEquals(397.0, item.y, 0.0)
        assertEquals(200.0, item.width, 0.0)
        assertEquals(60.0, item.height, 0.0)
    }

    @Test
    fun missingOnlyOneCoordinateCentersBoth() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(x = 10.0), 1, a4)
        assertEquals(217.5, item.x, 0.0)
        assertEquals(397.0, item.y, 0.0)
    }

    @Test
    fun explicitPositionAndEmptyLabel() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(x = 100.0, y = 200.0, label = ""), 2, a4)
        assertEquals(100.0, item.x, 0.0)
        assertEquals(200.0, item.y, 0.0)
        assertEquals(2, item.page)
        assertEquals("Signature", item.label)
    }

    @Test
    fun lockFlagsAreCopied() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(disableDrag = true, disableResize = true, locked = true), 1, a4)
        assertTrue(item.disableDrag && item.disableResize && item.locked)
        assertFalse(item.disableDragging)
    }

    @Test
    fun placeEmitsMatchingEventAndUpdatesList() {
        val signature = store.place(ItemType.SIGNATURE, PlaceOptions(), 1, a4)
        val stamp = store.place(ItemType.ESTAMP, PlaceOptions(), 1, a4)
        assertEquals(listOf(PsdkEvent.SignaturePlaced(signature), PsdkEvent.EStampPlaced(stamp)), events)
        assertEquals(listOf(signature, stamp), lastList)
    }

    @Test
    fun imageLoadSetsHeightFromAspectRatioAndEmitsMoved() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(image = PsdkImage.of("data:image/png;base64,AA")), 1, a4)
        events.clear()

        store.onImageLoaded(item.id, imageWidth = 400, imageHeight = 100)

        val updated = store.get(item.id)!!
        assertEquals(40.0, updated.height, 0.0)
        assertEquals(listOf(PsdkEvent.SignatureMoved(updated)), events)
    }

    @Test
    fun imageLoadAlsoReshapesEStamp() {
        val item = store.place(ItemType.ESTAMP, PlaceOptions(), 1, a4)
        store.onImageLoaded(item.id, imageWidth = 100, imageHeight = 120)
        assertEquals(96.0, store.get(item.id)!!.height, 1e-9)
    }

    @Test
    fun imageLoadKeepsExplicitHeight() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(height = 50.0), 1, a4)
        events.clear()
        store.onImageLoaded(item.id, 400, 100)
        assertEquals(50.0, store.get(item.id)!!.height, 0.0)
        assertTrue(events.isEmpty())
    }

    @Test
    fun moveEmitsMovedWithNewBox() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(), 1, a4)
        events.clear()
        val moved = store.move(item.id, ItemBox(10.0, 20.0, 100.0, 30.0))!!
        assertEquals(10.0, moved.x, 0.0)
        assertEquals(30.0, moved.height, 0.0)
        assertEquals(listOf(PsdkEvent.SignatureMoved(moved)), events)
    }

    @Test
    fun removeEmitsOnlyForKnownIds() {
        val item = store.place(ItemType.SIGNATURE, PlaceOptions(), 1, a4)
        events.clear()
        assertFalse(store.remove("nope"))
        assertTrue(events.isEmpty())
        assertTrue(store.remove(item.id))
        assertEquals(listOf(PsdkEvent.SignatureRemoved(item.id)), events)
        assertNull(store.get(item.id))
    }

    @Test
    fun clearAlwaysEmits() {
        store.clear()
        store.place(ItemType.SIGNATURE, PlaceOptions(), 1, a4)
        store.clear()
        assertEquals(PsdkEvent.SignaturesCleared, events.first())
        assertEquals(PsdkEvent.SignaturesCleared, events.last())
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun byPageFiltersAndKeepsOrder() {
        val a = store.place(ItemType.SIGNATURE, PlaceOptions(page = 1), 1, a4)
        store.place(ItemType.SIGNATURE, PlaceOptions(page = 2), 2, a4)
        val c = store.place(ItemType.ESTAMP, PlaceOptions(page = 1), 1, a4)
        assertEquals(listOf(a, c), store.byPage(1))
    }
}
