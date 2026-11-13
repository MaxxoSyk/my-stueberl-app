package de.landstueberl.mystueberlapp.data

/**
 * Lifecycle of a sale event. Every transition is made by hand.
 *
 * Stored by constant name, like [ProductStatus]: renaming a constant will not
 * break compilation, but will silently break SQL queries that use the name as
 * a string literal.
 */
enum class EventStatus {
    /** Created with its details only. Products cannot be assigned yet. */
    PLANNED,

    /** Products are being prepared and can be assigned. */
    IN_PREPARATION,

    /** The event is running. Later the point-of-sale screen opens here. */
    IN_PROGRESS,

    /** Finished. Links remain as history; unsold products are stand stock again. */
    DONE,

    /** Did not take place. Links remain; the products are free again. */
    CANCELLED;

    /**
     * Whether the event still holds its products. A product can belong to at
     * most one active event at a time, but to any number of finished ones.
     */
    val isActive: Boolean
        get() = this == PLANNED || this == IN_PREPARATION || this == IN_PROGRESS

    /** Only events that haven't started can be cancelled. */
    val isCancellable: Boolean
        get() = this == PLANNED || this == IN_PREPARATION

    /** Labels are printed while preparing, or at the market to replace a lost one. */
    val canPrintLabels: Boolean
        get() = this == IN_PREPARATION || this == IN_PROGRESS

    companion object {
        /**
         * Status names for SQL parameters. Queries take these instead of
         * spelling out literals, so "active" is defined only by [isActive].
         */
        val ACTIVE_NAMES: List<String> = entries.filter { it.isActive }.map { it.name }

        val CANCELLABLE_NAMES: List<String> = entries.filter { it.isCancellable }.map { it.name }

        val LABEL_PRINTABLE_NAMES: List<String> = entries.filter { it.canPrintLabels }.map { it.name }
    }
}