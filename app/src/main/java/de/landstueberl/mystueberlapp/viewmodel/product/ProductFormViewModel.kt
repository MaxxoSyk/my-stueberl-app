package de.landstueberl.mystueberlapp.viewmodel.product

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Currency
import kotlin.coroutines.cancellation.CancellationException

data class ProductFormValues(
    val description: String,
    val purchasePrice: Money?,
    val salesPrice: Money?
)

abstract class ProductFormViewModel(
    protected val repo: ProductRepository
) : ViewModel() {

    // ── Form Fields ────────────────────────────
    protected val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    protected val _purchasePrice = MutableStateFlow("")
    val purchasePrice: StateFlow<String> = _purchasePrice.asStateFlow()

    protected val _salesPrice = MutableStateFlow("")
    val salesPrice: StateFlow<String> = _salesPrice.asStateFlow()

    protected val _currency = MutableStateFlow(DEFAULT_CURRENCY)
    val currency: StateFlow<Currency> = _currency.asStateFlow()

    // ── UI State ───────────────────────────────
    protected val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    protected val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── Available Currencies ───────────────────
    val availableCurrencies: List<Currency> = AVAILABLE_CURRENCIES

    // ── Validation ─────────────────────────────
    /** Observable, for enabling the Save button. */
    val isSaveEnabled: StateFlow<Boolean> = _description
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Non-observable, for guarding [saveProduct]. Always current. */
    private val canSave: Boolean
        get() = _description.value.isNotBlank()

    // ── Field Updates ──────────────────────────
    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun onPurchasePriceChange(value: String) {
        if (value.isEmpty() || value.matches(PRICE_PATTERN)) {
            _purchasePrice.value = value
        }
    }

    fun onSalesPriceChange(value: String) {
        if (value.isEmpty() || value.matches(PRICE_PATTERN)) {
            _salesPrice.value = value
        }
    }

    fun onCurrencyChange(value: Currency) {
        _currency.value = value
    }

    // ── Save ───────────────────────────────────
    fun saveProduct() {
        if (!canSave || _isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                persist(
                    ProductFormValues(
                        description = _description.value.trim(),
                        purchasePrice = _purchasePrice.value.toMoneyOrNull(),
                        salesPrice = _salesPrice.value.toMoneyOrNull()
                    )
                )
                _isSaved.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save product", e)
                _isError.value = true
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Writes [values] to the repository. Implemented by subclasses:
     * insert for a new product, update for an existing one.
     */
    protected abstract suspend fun persist(values: ProductFormValues)

    private fun String.toMoneyOrNull(): Money? =
        takeIf { it.isNotBlank() }
            ?.toBigDecimalOrNull()
            ?.let { Money(it, _currency.value) }

    // ── Reset ──────────────────────────────────
    fun resetSavedState() {
        _isSaved.value = false
    }

    fun resetErrorState() {
        _isError.value = false
    }

    companion object {
        private const val TAG = "ProductFormViewModel"

        private val PRICE_PATTERN = Regex("^\\d*\\.?\\d*$")

        val DEFAULT_CURRENCY: Currency = Currency.getInstance("EUR")

        val AVAILABLE_CURRENCIES: List<Currency> = listOf(
            Currency.getInstance("EUR")
        )
    }
}