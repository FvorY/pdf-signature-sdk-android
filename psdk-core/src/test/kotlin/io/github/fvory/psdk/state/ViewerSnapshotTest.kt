package io.github.fvory.psdk.state

import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.SignatureItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ViewerSnapshotTest {
    private val snapshot =
        ViewerSnapshot(
            tempId = "c0ffee",
            documentPath = "/data/user/0/app/cache/psdk/c0ffee/doc.pdf",
            page = 3,
            scale = 1.25f,
            paginationLocked = true,
            scrollIndex = 2,
            scrollOffset = 140,
            scrollHorizontal = 30,
            items =
                listOf(
                    SignatureItem("signature-1-aaaaa", ItemType.SIGNATURE, 217.5, 397.0, 1, "Direktur \"A\"", 160.0, 48.0, locked = true),
                    SignatureItem("estamp-2-bbbbb", ItemType.ESTAMP, 10.25, 20.0, 3, "E-Materai", 80.0, 96.0, disableResize = true),
                ),
            itemImages = mapOf("signature-1-aaaaa" to "/cache/psdk/c0ffee/images/signature-1-aaaaa.png"),
        )

    @Test
    fun roundTrip() {
        assertEquals(snapshot, ViewerSnapshot.fromJson(snapshot.toJson()))
    }

    @Test
    fun noDocument() {
        val empty = snapshot.copy(documentPath = null, items = emptyList(), itemImages = emptyMap())
        assertEquals(empty, ViewerSnapshot.fromJson(empty.toJson()))
    }

    @Test
    fun missingOrDamagedStartsFresh() {
        assertNull(ViewerSnapshot.fromJson(null))
        assertNull(ViewerSnapshot.fromJson(""))
        assertNull(ViewerSnapshot.fromJson("{not json"))
        assertNull(ViewerSnapshot.fromJson("""{"v":99}"""))
    }
}
