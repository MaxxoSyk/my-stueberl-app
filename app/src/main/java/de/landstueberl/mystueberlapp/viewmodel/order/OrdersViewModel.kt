package de.landstueberl.mystueberlapp.viewmodel.order

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.Order
import de.landstueberl.mystueberlapp.data.OrderStatus
import de.landstueberl.mystueberlapp.repository.OrderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModel(
    private val repo: OrderRepository
) : ViewModel() {

    // ── Status Filter ──────────────────────────
    private val _statusFilter = MutableStateFlow(DEFAULT_STATUSES)
    val statusFilter: StateFlow<Set<OrderStatus>> = _statusFilter.asStateFlow()

    fun toggleStatusFilter(status: OrderStatus) {
        val current = _statusFilter.value.toMutableSet()
        if (current.contains(status)) {
            // Never allow an empty set — it would match nothing.
            if (current.size > 1) current.remove(status)
        } else {
            current.add(status)
        }
        _statusFilter.value = current
    }

    fun resetFilter() {
        _statusFilter.value = DEFAULT_STATUSES
    }

    /** Whether the filter can hide rows — drives the empty-state message. */
    val isFilterNarrowing: StateFlow<Boolean> = _statusFilter
        .map { it.size < OrderStatus.entries.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    // ── Orders List ────────────────────────────
    val orders: StateFlow<List<Order>> = _statusFilter
        .flatMapLatest { statuses -> repo.getOrdersFlow(statuses) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // ── Error Reporting ────────────────────────
    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun resetErrorState() { _isError.value = false }

    // ── Actions ────────────────────────────────
    fun deleteOrder(orderId: Int) = launchCatching {
        repo.deleteOrder(orderId)
    }

    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Database operation failed", e)
                _isError.value = true
            }
        }
    }

    companion object {
        private const val TAG = "OrdersViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Open orders only — the day-to-day working set. */
        private val DEFAULT_STATUSES = setOf(OrderStatus.OPEN)

        fun factory(repo: OrderRepository) = viewModelFactory {
            initializer<OrdersViewModel> { OrdersViewModel(repo) }
        }
    }
}