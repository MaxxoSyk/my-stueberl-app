package de.landstueberl.mystueberlapp.viewmodel.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.ProductSourceFilter
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductFilter
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModel(
    private val repo: ProductRepository
) : ViewModel() {

    // ── Filter State ───────────────────────────
    private val _filter = MutableStateFlow(ProductFilter())
    val filter: StateFlow<ProductFilter> = _filter

    // ── Available Years ────────────────────────
    val availableYears: StateFlow<List<Int>> = repo
        .getAvailableYearsFlow()
        .map { years -> years.map { it.toInt() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(LocalDate.now().year)
        )

    // ── Active Filter Count ────────────────────
    val activeFilterCount: StateFlow<Int> = _filter
        .map { filter ->
            var count = 0
            count += filter.years.size
            count += filter.statuses.size
            count += 1
            count
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // ── Filter Bottom Sheet ────────────────────
    private val _isFilterSheetVisible = MutableStateFlow(false)
    val isFilterSheetVisible: StateFlow<Boolean> = _isFilterSheetVisible

    fun showFilterSheet() { _isFilterSheetVisible.value = true }
    fun hideFilterSheet() { _isFilterSheetVisible.value = false }

    // ── Products List ──────────────────────────
    val products: StateFlow<List<Product>> = _filter
        .flatMapLatest { filter ->
            repo.getFilteredProductsFlow(filter)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ── Filter Updates ─────────────────────────
    fun toggleYearFilter(year: Int) {
        val current = _filter.value.years.toMutableSet()
        if (current.contains(year)) {
            current.remove(year)
        } else {
            current.add(year)
        }
        _filter.value = _filter.value.copy(years = current)
    }

    fun toggleStatusFilter(status: ProductStatus) {
        val current = _filter.value.statuses.toMutableSet()
        if (current.contains(status)) {
            if (current.size > 1) {
                current.remove(status)
            }
        } else {
            current.add(status)
        }
        _filter.value = _filter.value.copy(statuses = current)
    }

    fun setOrderFilter(productSourceFilter: ProductSourceFilter) {
        _filter.value = _filter.value.copy(productSourceFilter = productSourceFilter)
    }

    fun resetFilter() {
        _filter.value = ProductFilter()
    }

    // ── Selection Mode ─────────────────────────
    private val _selectedProducts = MutableStateFlow<Set<Int>>(emptySet())
    val selectedProducts: StateFlow<Set<Int>> = _selectedProducts

    // ── Bottom Sheet ───────────────────────────
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct

    fun onProductTap(product: Product) {
        _selectedProduct.value = product
    }

    fun dismissBottomSheet() {
        _selectedProduct.value = null
    }

    // ── Product Status ─────────────────────────
    fun markAsSold(productId: Int) {
        viewModelScope.launch {
            repo.markProductAsSold(productId)
            dismissBottomSheet()
        }
    }

    fun markAsRemoved(productId: Int) {
        viewModelScope.launch {
            repo.markProductAsRemoved(productId)
            dismissBottomSheet()
        }
    }

    fun resetToAvailable(productId: Int) {
        viewModelScope.launch {
            repo.resetProductToAvailable(productId)
            dismissBottomSheet()
        }
    }

    // ── Selection Mode ─────────────────────────
    fun toggleSelection(productId: Int) {
        val current = _selectedProducts.value.toMutableSet()
        if (current.contains(productId)) {
            current.remove(productId)
        } else {
            current.add(productId)
        }
        _selectedProducts.value = current
    }

    fun clearSelection() {
        _selectedProducts.value = emptySet()
    }

    fun deleteSelectedProducts() {
        viewModelScope.launch {
            repo.deleteProductsByIds(_selectedProducts.value.toList())
            clearSelection()
        }
    }

}