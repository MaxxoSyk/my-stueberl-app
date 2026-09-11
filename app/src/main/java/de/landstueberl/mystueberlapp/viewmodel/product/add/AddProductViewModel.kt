package de.landstueberl.mystueberlapp.viewmodel.product.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

class AddProductViewModel(
    private val repo: ProductRepository
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

    // ── UI State ───────────────────────────────
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    // ── Available Currencies ───────────────────
    val availableCurrencies = listOf(
        Currency.getInstance("EUR")
    )

    // ── Validation ─────────────────────────────
    val isSaveEnabled: Boolean
        get() = _description.value.isNotBlank()

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
                val purchasePrice = _purchasePrice.value
                    .takeIf { it.isNotBlank() }
                    ?.let { Money(BigDecimal(it), _currency.value) }

                val salesPrice = _salesPrice.value
                    .takeIf { it.isNotBlank() }
                    ?.let { Money(BigDecimal(it), _currency.value) }

                val product = Product(
                    details = ProductDetail(
                        id = 0,
                        description = _description.value.trim(),
                        purchasePrice = purchasePrice,
                        salesPrice = salesPrice,
                        isSold = false,
                        isRemoved = false
                    ),
                    imageList = emptyList()
                )

                repo.upsertProduct(product)
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