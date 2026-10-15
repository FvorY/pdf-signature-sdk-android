package io.github.fvory.psdk.core

import io.github.fvory.psdk.event.PsdkEvent
import io.github.fvory.psdk.model.ItemType
import io.github.fvory.psdk.model.PageSize
import io.github.fvory.psdk.model.PlaceOptions
import io.github.fvory.psdk.model.PsdkImage
import io.github.fvory.psdk.model.SignatureItem

/**
 * Placed signatures and E-Materai, in PDF points (B-CRD-01). The only place items change;
 * every change reports the new list through [onChanged] and sends the web SDK's event.
 */
internal class ItemStore(
    private val ids: IdGenerator,
    private val onChanged: (List<SignatureItem>) -> Unit,
    private val emit: (PsdkEvent) -> Unit,
) {
    private var items: List<SignatureItem> = emptyList()
    private val images = mutableMapOf<String, PsdkImage>()
    private val explicitHeight = mutableSetOf<String>()

    fun all(): List<SignatureItem> = items

    fun byPage(page: Int): List<SignatureItem> = items.filter { it.page == page }

    fun get(id: String): SignatureItem? = items.firstOrNull { it.id == id }

    fun imageOf(id: String): PsdkImage? = images[id]

    /**
     * Creates an item on [page] of size [pageSize] (B-OVL-03…07). The page must already be valid.
     * Missing x or y centers the item using the default size, even when a size is given (web behavior).
     */
    fun place(
        type: ItemType,
        options: PlaceOptions,
        page: Int,
        pageSize: PageSize,
    ): SignatureItem {
        val isStamp = type == ItemType.ESTAMP
        val defaultWidth = if (isStamp) ESTAMP_SIZE else SIGNATURE_WIDTH
        val defaultHeight = if (isStamp) ESTAMP_SIZE else SIGNATURE_HEIGHT
        val centered = options.x == null || options.y == null
        val item =
            SignatureItem(
                id = ids.next(type),
                type = type,
                x = if (centered) pageSize.width / 2 - defaultWidth / 2 else options.x!!,
                y = if (centered) pageSize.height / 2 - defaultHeight / 2 else options.y!!,
                page = page,
                label = if (isStamp) ESTAMP_LABEL else options.label?.takeIf { it.isNotEmpty() } ?: SIGNATURE_LABEL,
                width = options.width ?: defaultWidth,
                height = options.height ?: defaultHeight,
                disableDrag = options.disableDrag,
                disableDragging = options.disableDragging,
                disableResize = options.disableResize,
                locked = options.locked,
            )
        options.image?.let { images[item.id] = it }
        if (options.height != null) explicitHeight += item.id
        set(items + item)
        emit(if (isStamp) PsdkEvent.EStampPlaced(item) else PsdkEvent.SignaturePlaced(item))
        return item
    }

    /** Commits a drag or resize and sends `signatureMoved` (B-INT-07). */
    fun move(
        id: String,
        box: ItemBox,
    ): SignatureItem? {
        val current = get(id) ?: return null
        val moved = current.copy(x = box.x, y = box.y, width = box.width, height = box.height)
        replace(moved)
        emit(PsdkEvent.SignatureMoved(moved))
        return moved
    }

    /**
     * An item's image finished loading. Without an explicit height, the height follows the image's
     * aspect ratio and `signatureMoved` is sent (B-OVL-06).
     */
    fun onImageLoaded(
        id: String,
        imageWidth: Int,
        imageHeight: Int,
    ) {
        val current = get(id) ?: return
        if (id in explicitHeight || imageWidth <= 0) return
        val resized = current.copy(height = current.width * imageHeight / imageWidth)
        replace(resized)
        emit(PsdkEvent.SignatureMoved(resized))
    }

    fun remove(id: String): Boolean {
        if (get(id) == null) return false
        images -= id
        explicitHeight -= id
        set(items.filterNot { it.id == id })
        emit(PsdkEvent.SignatureRemoved(id))
        return true
    }

    /** Puts items back after process death, without events (spec §17). */
    fun restore(restored: List<SignatureItem>) {
        images.clear()
        explicitHeight.clear()
        // Restored items keep their saved size: a late image load must not change the height
        explicitHeight += restored.map { it.id }
        set(restored)
    }

    /** Removes everything and always sends `signaturesCleared` (B-OVL-10). */
    fun clear() {
        images.clear()
        explicitHeight.clear()
        set(emptyList())
        emit(PsdkEvent.SignaturesCleared)
    }

    private fun replace(item: SignatureItem) {
        set(items.map { if (it.id == item.id) item else it })
    }

    private fun set(newItems: List<SignatureItem>) {
        items = newItems
        onChanged(newItems)
    }

    companion object {
        const val SIGNATURE_WIDTH: Double = 160.0
        const val SIGNATURE_HEIGHT: Double = 48.0
        const val ESTAMP_SIZE: Double = 80.0
        const val SIGNATURE_LABEL: String = "Signature"
        const val ESTAMP_LABEL: String = "E-Materai"
    }
}
