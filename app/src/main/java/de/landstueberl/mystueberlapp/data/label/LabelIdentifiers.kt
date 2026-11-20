package de.landstueberl.mystueberlapp.data.label

import kotlin.random.Random

/** Short, easy-to-read identifiers printed under the QR code. */
object LabelIdentifiers {

    /** No look-alikes: 0/O, 1/I/L are left out. 31 characters. */
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val LENGTH = 4

    /**
     * A new identifier not in [taken]. With ~920,000 combinations and at most a
     * few hundred per event, a clash is rare; it just tries again.
     */
    fun generate(taken: Set<String>, random: Random = Random.Default): String {
        while (true) {
            val candidate = buildString(LENGTH) {
                repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
            }
            if (candidate !in taken) return candidate
        }
    }
}