package de.landstueberl.mystueberlapp.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository
): ViewModel() {
    /** Sales-area products still available. */
    val availableSalesAreaCount: StateFlow<Int> = productRepo
        .getAvailableSalesAreaCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    /** Products made for orders that haven't been resolved yet. */
    val pendingOrderProductsCount: StateFlow<Int> = productRepo
        .getPendingOrderProductsCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    /** All products created this year, both sources. */
    val totalProductsCurrentYear: StateFlow<Int> = productRepo
        .getTotalProductsForYearFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            productRepository: ProductRepository,
            orderRepository: OrderRepository
        ) = viewModelFactory {
            initializer<HomeViewModel> {
                HomeViewModel(
                    productRepo = productRepository,
                    orderRepo = orderRepository
                )
            }
        }
    }
}