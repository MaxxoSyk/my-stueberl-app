package de.landstueberl.mystueberlapp.viewmodel.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Product IDs ticked in an attach picker. Shared by orders and events, so
 * single and batch toggling behave the same everywhere.
 */
class AttachSelection {

    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds: StateFlow<Set<Int>> = _selectedIds.asStateFlow()

    val current: Set<Int>
        get() = _selectedIds.value

    fun toggle(productId: Int) = _selectedIds.update { current ->
        if (productId in current) current - productId else current + productId
    }

    /** Ticks every member of a batch, or unticks all if they're all ticked already. */
    fun toggleBatch(productIds: List<Int>) = _selectedIds.update { current ->
        val ids = productIds.toSet()
        if (current.containsAll(ids)) current - ids else current + ids
    }

    fun clear() {
        _selectedIds.value = emptySet()
    }
}