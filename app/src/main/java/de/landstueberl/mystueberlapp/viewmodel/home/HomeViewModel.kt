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

    // @todo decide what FAB on HomeScreen should do

    // ── Available Products Count ───────────────
    val availableProductsCount: StateFlow<Int> = productRepo
        .getAvailableProductsCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalProductsCurrentYear: StateFlow<Int> = productRepo
        .getTotalProductsForYearFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    companion object {
        fun factory(
            productRepository: ProductRepository,
            orderRepository: OrderRepository
        ) = viewModelFactory {
            initializer {
                HomeViewModel(
                    productRepo = productRepository,
                    orderRepo = orderRepository
                )
            }
        }
    }
}