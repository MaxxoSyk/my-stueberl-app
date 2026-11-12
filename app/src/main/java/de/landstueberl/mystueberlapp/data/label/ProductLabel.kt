package de.landstueberl.mystueberlapp.data.label

/** One label on the sheet: the QR code and the caption printed under it. */
data class ProductLabel(
    val code: ProductLabelCode,
    /** Lets you match a cut-out label to its product; ellipsized to fit. */
    val caption: String
)