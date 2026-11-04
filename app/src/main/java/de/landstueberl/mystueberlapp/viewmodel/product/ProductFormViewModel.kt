package de.landstueberl.mystueberlapp.viewmodel.product

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.data.calculatedTotal
import de.landstueberl.mystueberlapp.data.db.entity.ProductMaterialDetail
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

data class ProductFormValues(
    val description: String,
    val purchasePrice: Money,
    val salesPrice: Money,
    val imageFileName: String?,
    val materials: List<ProductMaterialDetail>
)

abstract class ProductFormViewModel(
    protected val repo: ProductRepository,
    private val imageStorage: ProductImageStorage
) : ViewModel() {

    // ── Image ──────────────────────────────────
    protected val _imageFileName = MutableStateFlow<String?>(null)
    val imageFileName: StateFlow<String?> = _imageFileName.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // ── Form Fields ────────────────────────────
    protected val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    protected val _purchasePrice = MutableStateFlow("")
    val purchasePrice: StateFlow<String> = _purchasePrice.asStateFlow()

    protected val _salesPrice = MutableStateFlow("")
    val salesPrice: StateFlow<String> = _salesPrice.asStateFlow()

    // ── Cost Calculation ───────────────────────
    /** The committed material list; empty means the price was entered by hand. */
    protected val _materials = MutableStateFlow<List<ProductMaterialDetail>>(emptyList())

    /**
     * Working copy while the calculator is open. Changes are applied to
     * [_materials] only on confirm, so dismissing discards them.
     */
    private val _materialDraft = MutableStateFlow<List<ProductMaterialDetail>>(emptyList())
    val materialDraft: StateFlow<List<ProductMaterialDetail>> = _materialDraft.asStateFlow()

    /** True when the purchase price comes from a calculation and is read-only. */
    val isPurchasePriceCalculated: StateFlow<Boolean> = _materials
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    // ── Material Name Suggestions ──────────────
    private val _materialNameSuggestions = MutableStateFlow<List<String>>(emptyList())
    val materialNameSuggestions: StateFlow<List<String>> =
        _materialNameSuggestions.asStateFlow()

    // ── UI State ───────────────────────────────
    protected val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    protected val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── Validation ─────────────────────────────
    /** Observable, for enabling the Save button. */
    val isSaveEnabled: StateFlow<Boolean> = _description
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /** Non-observable, for guarding [saveProduct]. Always current. */
    private val canSave: Boolean
        get() = _description.value.isNotBlank()

    /** The current form contents, trimmed and parsed. */
    protected fun currentFormValues(): ProductFormValues = ProductFormValues(
        description = _description.value.trim(),
        purchasePrice = _purchasePrice.value.toMoney(),
        salesPrice = _salesPrice.value.toMoney(),
        imageFileName = _imageFileName.value,
        materials = _materials.value
    )

    // ── Field Updates ──────────────────────────
    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun onPurchasePriceChange(value: String) {
        if (PriceInput.isValid(value)) {
            _purchasePrice.value = value
        }
    }

    fun onSalesPriceChange(value: String) {
        if (PriceInput.isValid(value)) {
            _salesPrice.value = value
        }
    }

    fun createCameraTarget(): ProductImageStorage.CameraTarget =
        imageStorage.createCameraTarget()

    /** Resolves a stored file name to a file Coil can load. */
    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    /**
     * Imports the image at [source] into internal storage and makes it the
     * product's image, replacing any previous one.
     */
    fun onImageSelected(source: Uri) {
        if (_isImporting.value) return

        viewModelScope.launch {
            _isImporting.value = true
            try {
                // The previous file is intentionally left on disk: it may still be
                // referenced by the database until the user actually saves.
                // Unreferenced files are removed by the orphan sweep at app start.
                _imageFileName.value = imageStorage.importImage(source)
                imageStorage.clearCameraTemp()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import image from $source", e)
                _isError.value = true
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun onImageRemoved() {
        // File deletion is deferred to the orphan sweep for the same reason.
        _imageFileName.value = null
    }

    // ── Save ───────────────────────────────────
    fun saveProduct() {
        if (!canSave || _isSaving.value || _isImporting.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                persist(currentFormValues())
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

    /**
     * Parses user input into [Money], treating an empty field as zero — a
     * product always has both prices, so there is no "not priced" state.
     */
    private fun String.toMoney(): Money = PriceInput.parseMoney(this)

    /**
     * Loads **all** previously used material names in one go, rather than
     * querying per keystroke.
     *
     * A single shop accumulates tens of distinct materials, not thousands, so
     * filtering in memory is cheaper than a debounced query per character and
     * avoids a coroutine on every edit. Revisit if the list ever grows large
     * enough to notice.
     */
    private fun loadMaterialNameSuggestions() {
        viewModelScope.launch {
            try {
                _materialNameSuggestions.value = repo.getAllMaterialNames()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Autocomplete is a convenience; failing silently is acceptable.
                Log.e(TAG, "Failed to load material names", e)
            }
        }
    }

    fun startMaterialDraft() {
        _materialDraft.value = _materials.value
        loadMaterialNameSuggestions()
    }

    /**
     * Commits the draft and writes the resulting total into the purchase price
     * field. An empty draft clears the calculation and empties the price,
     * leaving it editable again.
     */
    fun confirmMaterialDraft() {
        val draft = _materialDraft.value
        _materials.value = draft
        _purchasePrice.value = draft.calculatedTotal()
            ?.amount
            ?.toPlainString()
            ?: ""
    }

    /** Discards the draft without touching the committed list. */
    fun cancelMaterialDraft() {
        _materialDraft.value = emptyList()
    }

    // ── Draft Editing ──────────────────────────
    // Rows are addressed by index: several new rows share id = 0 until saved,
    // so the id is not a usable key here.

    fun addDraftPosition(position: ProductMaterialDetail) {
        _materialDraft.value = _materialDraft.value + position
    }

    fun updateDraftPosition(index: Int, position: ProductMaterialDetail) {
        val current = _materialDraft.value
        if (index !in current.indices) return
        _materialDraft.value = current.toMutableList().apply { set(index, position) }
    }

    fun removeDraftPosition(index: Int) {
        val current = _materialDraft.value
        if (index !in current.indices) return
        _materialDraft.value = current.toMutableList().apply { removeAt(index) }
    }

    /** Empties the draft; confirming then clears the calculation entirely. */
    fun clearDraft() {
        _materialDraft.value = emptyList()
    }

    // ── Reset ──────────────────────────────────
    fun resetSavedState() {
        _isSaved.value = false
    }

    fun resetErrorState() {
        _isError.value = false
    }

    companion object {
        private const val TAG = "ProductFormViewModel"

        /** How long a shared flow stays active after its last collector. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}