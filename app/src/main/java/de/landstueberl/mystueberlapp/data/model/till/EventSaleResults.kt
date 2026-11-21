package de.landstueberl.mystueberlapp.data.model.till

import de.landstueberl.mystueberlapp.data.db.entity.EventSale

/** Outcome of adding a product to the till's basket. */
sealed interface BasketAddResult {
    data object Added : BasketAddResult
    data object EventNotRunning : BasketAddResult
    data object NotInEvent : BasketAddResult
    /** Sold or removed already. */
    data object NotAvailable : BasketAddResult
    data object AlreadyInBasket : BasketAddResult
    /** Group label: every member of the batch is sold or already in the basket. */
    data object BatchEmpty : BasketAddResult
}

/** Outcome of completing a sale. */
sealed interface SaleResult {
    data class Completed(val sale: EventSale) : SaleResult
    data object EventNotRunning : SaleResult
    data object EmptyBasket : SaleResult
    /**
     * The basket differed from what the screen showed (e.g. an item was sold
     * elsewhere). Outdated items were removed; nothing was sold. Check the
     * total with the customer and pay again.
     */
    data object BasketChanged : SaleResult
}