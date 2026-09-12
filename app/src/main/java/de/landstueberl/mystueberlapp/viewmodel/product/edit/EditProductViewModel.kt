package de.landstueberl.mystueberlapp.viewmodel.product.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

class EditProductViewModel(
    private val repo: ProductRepository,
    private val productId: Int
) : ViewModel() {

    // ── Form Fields ────────────────────────────
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _purchasePrice = MutableStateFlow("")
    val purchasePrice: StateFlow<String> = _purchasePrice

    private val _salesPrice = MutableStateFlow("")
    val salesPrice: StateFlow<String> = _salesPrice

    private val _currency = MutableStateFlow(Currency.getInstance("EUR"))
    val currency: StateFlow<Currency> = _currency

    // ── Read Only Fields ───────────────────────
    private val _createdAt = MutableStateFlow<LocalDate?>(null)
    val createdAt: StateFlow<LocalDate?> = _createdAt

    private val _removedOn = MutableStateFlow<LocalDate?>(null)
    val removedOn: StateFlow<LocalDate?> = _removedOn

    private val _isSold = MutableStateFlow(false)
    val isSold: StateFlow<Boolean> = _isSold

    private val _isRemoved = MutableStateFlow(false)
    val isRemoved: StateFlow<Boolean> = _isRemoved

    // ── UI State ───────────────────────────────
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ── Available Currencies ───────────────────
    val availableCurrencies = listOf(
        Currency.getInstance("EUR"),
        Currency.getInstance("USD"),
        Currency.getInstance("GBP"),
        Currency.getInstance("CHF")
    )

    // ── Validation ─────────────────────────────
    val isSaveEnabled: Boolean
        get() = _description.value.isNotBlank()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            try {
                val product = repo.getProductById(productId)
                // ── Prefill form fields ──
                _description.value = product.details.description
                _purchasePrice.value = product.details.purchasePrice
                    ?.amount?.toPlainString() ?: ""
                _salesPrice.value = product.details.salesPrice
                    ?.amount?.toPlainString() ?: ""
                _currency.value = product.details.purchasePrice
                    ?.currency ?: Currency.getInstance("EUR")
                _createdAt.value = product.details.createdAt
                _removedOn.value = product.details.removedOn
                _isSold.value = product.details.isSold
                _isRemoved.value = product.details.isRemoved
                _isLoading.value = false
            } catch (e: Exception) {
                _isError.value = true
                _isLoading.value = false
            }
        }
    }

    // ── Field Updates ──────────────────────────
    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun onPurchasePriceChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _purchasePrice.value = value
        }
    }

    fun onSalesPriceChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _salesPrice.value = value
        }
    }

    fun onCurrencyChange(value: Currency) {
        _currency.value = value
    }

    // ── Save Product ───────────────────────────
    fun saveProduct() {
        if (!isSaveEnabled) return

        viewModelScope.launch {
            try {
                val existingProduct = repo.getProductById(productId)

                val purchasePrice = _purchasePrice.value
                    .takeIf { it.isNotBlank() }
                    ?.let { Money(BigDecimal(it), _currency.value) }

                val salesPrice = _salesPrice.value
                    .takeIf { it.isNotBlank() }
                    ?.let { Money(BigDecimal(it), _currency.value) }

                val updatedProduct = Product(
                    details = existingProduct.details.copy(
                        description = _description.value.trim(),
                        purchasePrice = purchasePrice,
                        salesPrice = salesPrice,
                    ),
                    imageList = existingProduct.imageList
                )

                repo.upsertProduct(updatedProduct)
                _isSaved.value = true

            } catch (e: Exception) {
                _isError.value = true
            }
        }
    }

    fun resetSavedState() {
        _isSaved.value = false
    }

    fun resetErrorState() {
        _isError.value = false
    }
}