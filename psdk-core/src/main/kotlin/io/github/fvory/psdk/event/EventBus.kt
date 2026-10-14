package io.github.fvory.psdk.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Synchronous event dispatcher (B-EVT-01, B-EVT-02).
 *
 * Listeners run in registration order inside [emit]. The same listener registered twice
 * for the same type is kept once. Events are also offered to [flow] for coroutine users.
 */
internal class EventBus {
    private class Entry(
        val type: Class<out PsdkEvent>,
        val listener: PsdkListener<PsdkEvent>,
        val once: Boolean,
    )

    private val entries = CopyOnWriteArrayList<Entry>()
    private val mutableFlow = MutableSharedFlow<PsdkEvent>(extraBufferCapacity = 64)

    val flow: SharedFlow<PsdkEvent> = mutableFlow.asSharedFlow()

    fun <E : PsdkEvent> on(
        type: Class<E>,
        listener: PsdkListener<E>,
        once: Boolean = false,
    ): PsdkSubscription {
        val existing = entries.firstOrNull { it.type == type && it.listener === listener }
        if (existing != null) return PsdkSubscription { entries.remove(existing) }

        @Suppress("UNCHECKED_CAST")
        val entry = Entry(type, listener as PsdkListener<PsdkEvent>, once)
        entries += entry
        return PsdkSubscription { entries.remove(entry) }
    }

    fun emit(event: PsdkEvent) {
        for (entry in entries) {
            if (!entry.type.isInstance(event)) continue
            if (entry.once) entries.remove(entry)
            entry.listener.onEvent(event)
        }
        mutableFlow.tryEmit(event)
    }

    fun clear() {
        entries.clear()
    }

    val listenerCount: Int get() = entries.size
}
