package de.landstueberl.mystueberlapp.viewmodel.product

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.ProductFilter
import de.landstueberl.mystueberlapp.data.ProductSourceFilter
import de.landstueberl.mystueberlapp.data.ProductStatus
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModel(
    private val repo: ProductRepository
) : ViewModel() {

    // ── Filter State ───────────────────────────
    private val _filter = MutableStateFlow(ProductFilter())
    val filter: StateFlow<ProductFilter> = _filter.asStateFlow()

    // ── Available Years ────────────────────────
    val availableYears: StateFlow<List<Int>> = repo
        .getAvailableYearsFlow()
        .map { years -> years.mapNotNull { it.toIntOrNull() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = listOf(LocalDate.now().year)
        )

    // ── Active Filter Count ────────────────────
    // Counts *categories* that deviate from the default filter, so a freshly
    // opened screen shows no badge.
    val activeFilterCount: StateFlow<Int> = _filter
        .map { current ->
            var count = 0
            if (current.years != DEFAULT_FILTER.years) count++
            if (current.statuses != DEFAULT_FILTER.statuses) count++
            if (current.productSourceFilter != DEFAULT_FILTER.productSourceFilter) count++
            count
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0
        )

    // ── Filter Bottom Sheet ────────────────────
    private val _isFilterSheetVisible = MutableStateFlow(false)
    val isFilterSheetVisible: StateFlow<Boolean> = _isFilterSheetVisible.asStateFlow()

    fun showFilterSheet() { _isFilterSheetVisible.value = true }
    fun hideFilterSheet() { _isFilterSheetVisible.value = false }

    // ── Products List ──────────────────────────
    val products: StateFlow<List<Product>> = _filter
        .flatMapLatest { filter -> repo.getFilteredProductsFlow(filter) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    // ── Selection Mode ─────────────────────────
    private val _selectedProducts = MutableStateFlow<Set<Int>>(emptySet())
    val selectedProducts: StateFlow<Set<Int>> = _selectedProducts.asStateFlow()

    // ── Bottom Sheet ───────────────────────────
    // Only the id is held; the Product is derived from the live list so the
    // sheet always reflects the current status and closes if the row vanishes.
    private val _selectedProductId = MutableStateFlow<Int?>(null)

    val selectedProduct: StateFlow<Product?> =
        combine(_selectedProductId, products) { id, list ->
            id?.let { wanted -> list.firstOrNull { it.details.id == wanted } }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    // ── Error Reporting ────────────────────────
    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun resetErrorState() { _isError.value = false }

    init {
        // A filter change can hide rows that are still selected; dropping the
        // selection prevents deleting products the user can no longer see.
        _filter
            .drop(1)
            .onEach { clearSelection() }
            .launchIn(viewModelScope)
    }

    // ── Filter Updates ─────────────────────────
    fun toggleYearFilter(year: Int) {
        val current = _filter.value.years.toMutableSet()
        if (!current.remove(year)) current.add(year)
        _filter.value = _filter.value.copy(years = current)
    }

    fun toggleStatusFilter(status: ProductStatus) {
        val current = _filter.value.statuses.toMutableSet()
        if (current.contains(status)) {
            // Never allow an empty status set — it would match nothing.
            if (current.size > 1) current.remove(status)
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

    // ── Bottom Sheet Actions ───────────────────
    fun onProductTap(product: Product) {
        _selectedProductId.value = product.details.id
    }

    fun dismissBottomSheet() {
        _selectedProductId.value = null
    }

    // ── Product Status ─────────────────────────
    fun markAsSold(productId: Int) = launchCatching {
        repo.markProductAsSold(productId)
    }

    fun markAsRemoved(productId: Int) = launchCatching {
        repo.markProductAsRemoved(productId)
    }

    fun resetToAvailable(productId: Int) = launchCatching {
        repo.resetProductToAvailable(productId)
    }

    // ── Selection ──────────────────────────────
    fun toggleSelection(productId: Int) {
        val current = _selectedProducts.value.toMutableSet()
        if (!current.remove(productId)) current.add(productId)
        _selectedProducts.value = current
    }

    fun clearSelection() {
        _selectedProducts.value = emptySet()
    }

    fun deleteSelectedProducts() = launchCatching {
        val ids = _selectedProducts.value.toList()
        if (ids.isEmpty()) return@launchCatching
        repo.deleteProductsByIds(ids)
        clearSelection()
    }

    /**
     * Runs [block] in [viewModelScope], reporting failures via [isError]
     * instead of crashing the process.
     */
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
        private const val TAG = "ProductsViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        private val DEFAULT_FILTER = ProductFilter()

        fun factory(repo: ProductRepository) = viewModelFactory {
            initializer<ProductsViewModel> { ProductsViewModel(repo) }
        }
    }
}