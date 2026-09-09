package de.landstueberl.mystueberlapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository

class HomeViewModelFactory(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                productRepo = productRepository,
                orderRepo = orderRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}