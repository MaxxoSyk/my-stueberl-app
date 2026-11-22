package de.landstueberl.mystueberlapp.data.label

/** One label on the sheet: the QR code and the caption printed under it. */
data class ProductLabel(
    /** A single product or a whole batch. */
    val code: LabelCode,
    /** Identifier first ("K7QM · Herbstkranz ×5"), so ellipsizing only cuts the description. */
    val caption: String
)