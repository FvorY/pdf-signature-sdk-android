package io.github.fvory.psdk.core

import io.github.fvory.psdk.model.ItemType
import kotlin.random.Random

/** Item ids in the web SDK format: `<type>-<epoch millis>-<5 random base-36 chars>` (B-OVL-01). */
internal class IdGenerator(
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: Random = Random.Default,
) {
    fun next(type: ItemType): String {
        val suffix = CharArray(5) { ALPHABET[random.nextInt(ALPHABET.length)] }.concatToString()
        return "${type.value}-${clock()}-$suffix"
    }

    private companion object {
        const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz"
    }
}
