package io.github.fvory.psdk.event

import org.junit.Assert.assertEquals
import org.junit.Test

/** TC-U23 — B-EVT-01, B-EVT-02. */
class EventBusTest {
    private val bus = EventBus()
    private val received = mutableListOf<String>()

    @Test
    fun deliversByTypeInRegistrationOrder() {
        bus.on(PsdkEvent.DocumentLoaded::class.java, { received += "a${it.totalPages}" })
        bus.on(PsdkEvent.PageChanged::class.java, { received += "page${it.page}" })
        bus.on(PsdkEvent.DocumentLoaded::class.java, { received += "b${it.totalPages}" })

        bus.emit(PsdkEvent.DocumentLoaded(3))
        bus.emit(PsdkEvent.PageChanged(2, 3, PsdkEvent.PageChanged.Source.API))

        assertEquals(listOf("a3", "b3", "page2"), received)
    }

    @Test
    fun sameListenerIsRegisteredOnce() {
        val listener = PsdkListener<PsdkEvent.SignaturesCleared> { received += "cleared" }
        bus.on(PsdkEvent.SignaturesCleared::class.java, listener)
        bus.on(PsdkEvent.SignaturesCleared::class.java, listener)

        bus.emit(PsdkEvent.SignaturesCleared)

        assertEquals(listOf("cleared"), received)
        assertEquals(1, bus.listenerCount)
    }

    @Test
    fun cancelStopsDelivery() {
        val subscription = bus.on(PsdkEvent.SignaturesCleared::class.java, { received += "x" })
        subscription.cancel()

        bus.emit(PsdkEvent.SignaturesCleared)

        assertEquals(emptyList<String>(), received)
    }

    @Test
    fun onceFiresOnlyOnce() {
        bus.on(PsdkEvent.DocumentLoaded::class.java, { received += "once" }, once = true)

        bus.emit(PsdkEvent.DocumentLoaded(1))
        bus.emit(PsdkEvent.DocumentLoaded(1))

        assertEquals(listOf("once"), received)
        assertEquals(0, bus.listenerCount)
    }

    @Test
    fun listenerMayRemoveItselfWhileDispatching() {
        lateinit var subscription: PsdkSubscription
        subscription =
            bus.on(PsdkEvent.SignaturesCleared::class.java, {
                received += "self"
                subscription.cancel()
            })
        bus.on(PsdkEvent.SignaturesCleared::class.java, { received += "other" })

        bus.emit(PsdkEvent.SignaturesCleared)
        bus.emit(PsdkEvent.SignaturesCleared)

        assertEquals(listOf("self", "other", "other"), received)
    }

    @Test
    fun clearRemovesEverything() {
        bus.on(PsdkEvent.SignaturesCleared::class.java, { received += "x" })
        bus.clear()
        bus.emit(PsdkEvent.SignaturesCleared)
        assertEquals(0, bus.listenerCount)
        assertEquals(emptyList<String>(), received)
    }
}
