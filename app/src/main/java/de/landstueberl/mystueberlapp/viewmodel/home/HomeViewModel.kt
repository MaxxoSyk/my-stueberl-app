package de.landstueberl.mystueberlapp.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Currency

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
}