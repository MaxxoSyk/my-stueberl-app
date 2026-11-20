package de.landstueberl.mystueberlapp.data.db.relation

import java.time.LocalDate

/** A product whose label for an event was confirmed as printed. */
data class LabelPrint(
    val productId: Int,
    val labelPrintedAt: LocalDate
)