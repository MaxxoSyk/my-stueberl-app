package de.landstueberl.mystueberlapp.viewmodel.order

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.Order
import de.landstueberl.mystueberlapp.data.OrderStatus
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.db.entity.OrderDetail
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.OrderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModel(
    private val repo: OrderRepository,
    private val imageStorage: ProductImageStorage,
    private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Null until the order has been persisted at least once. */
    private val _orderId: MutableStateFlow<Int?> = MutableStateFlow(
        savedStateHandle.toRoute<Screen.OrderDetail>().orderId
    )

    /**
     * False while the order exists only because "Add product" needed an id.
     * See [discardIfAbandoned].
     */
    private var wasExplicitlySaved: Boolean = _orderId.value != null

    /** Set when the order has been deleted, so cleanup doesn't run again. */
    private var isDiscarded: Boolean = false

    val isNewOrder: Boolean = _orderId.value == null

    // ── Form Fields ────────────────────────────
    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _dueDate = MutableStateFlow<LocalDate?>(null)
    val dueDate: StateFlow<LocalDate?> = _dueDate.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    /**
     * The persisted order, or null for one that doesn't exist yet.
     *
     * Eagerly shared because [persist] reads [StateFlow.value]: with
     * WhileSubscribed the flow could have stopped and reverted to null,
     * which would reset a resolved order's status on save.
     */
    private val persistedOrder: StateFlow<Order?> = _orderId
        .flatMapLatest { id: Int? ->
            if (id == null) flowOf(null) else repo.getOrderFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Read Only State ───────────────────────
    // Derived from the database, so status transitions reflect immediately.
    val status: StateFlow<OrderStatus> = persistedOrder
        .map { it?.orderDetail?.status ?: OrderStatus.OPEN }
        .stateIn(viewModelScope, SharingStarted.Eagerly, OrderStatus.OPEN)

    val createdAt: StateFlow<LocalDate?> = persistedOrder
        .map { it?.orderDetail?.createdAt }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val resolvedOn: StateFlow<LocalDate?> = persistedOrder
        .map { it?.orderDetail?.resolvedOn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val cancelledOn: StateFlow<LocalDate?> = persistedOrder
        .map { it?.orderDetail?.cancelledOn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val isEditable: StateFlow<Boolean> = status
        .map { it == OrderStatus.OPEN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    // ── Products ───────────────────────────────
    // Observed live, so attaching or removing a product updates immediately.
    val products: StateFlow<List<Product>> = persistedOrder
        .map { it?.productList ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── UI State ───────────────────────────────
    private val _isLoading = MutableStateFlow(!isNewOrder)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    // ── Validation ─────────────────────────────
    /** An order needs a customer and a promise date before it can be stored. */
    private val canSave: Boolean
        get() = _customerName.value.isNotBlank() && _dueDate.value != null

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _customerName,
        _dueDate
    ) { name, due ->
        name.isNotBlank() && due != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /** An order with no products is almost certainly not finished. */
    val isResolveEnabled: StateFlow<Boolean> = combine(
        status,
        products
    ) { orderStatus, productList ->
        orderStatus == OrderStatus.OPEN && productList.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    init {
        if (!isNewOrder) loadOrder()
    }

    // ── Loading ────────────────────────────────
    private fun loadOrder() {
        val id = _orderId.value ?: return
        Log.d(TAG, "loadOrder: starting for id=$id")

        viewModelScope.launch {
            _isLoading.value = true
            _loadFailed.value = false
            try {
                val order = repo.getOrder(id)
                Log.d(TAG, "loadOrder: got ${order?.orderDetail?.customerName}")
                if (order == null) {
                    _loadFailed.value = true
                    return@launch
                }
                applyToForm(order)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load order $id", e)
                _loadFailed.value = true
            } finally {
                Log.d(TAG, "loadOrder: finished, isLoading=false")
                _isLoading.value = false
            }
        }
    }

    private fun applyToForm(order: Order) {
        val detail = order.orderDetail
        _customerName.value = detail.customerName
        _dueDate.value = detail.dueDate
        _note.value = detail.note
    }

    fun retry() {
        loadOrder()
    }

    // ── Field Updates ──────────────────────────
    fun onCustomerNameChange(value: String) {
        _customerName.value = value
    }

    fun onDueDateChange(value: LocalDate) {
        _dueDate.value = value
    }

    fun onNoteChange(value: String) {
        _note.value = value
    }

    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    fun resetErrorState() { _isError.value = false }

    // ── Persistence ────────────────────────────
    /** Writes the order fields, inserting the row if it doesn't exist yet. */
    private suspend fun persist(): Int {
        val existing = persistedOrder.value?.orderDetail

        val detail = OrderDetail(
            id = _orderId.value ?: 0,
            customerName = _customerName.value.trim(),
            dueDate = requireNotNull(_dueDate.value) { "dueDate must be set before saving" },
            note = _note.value.trim(),
            status = existing?.status ?: OrderStatus.OPEN,
            createdAt = existing?.createdAt ?: LocalDate.now(),
            resolvedOn = existing?.resolvedOn,
            cancelledOn = existing?.cancelledOn
        )

        val id = repo.upsertOrderDetail(detail)
        _orderId.value = id
        return id
    }

    /** Explicit Save — commits the order and marks it deliberately kept. */
    fun saveOrder(onSaved: () -> Unit) {
        if (!canSave || _isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                persist()
                wasExplicitlySaved = true
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save order", e)
                _isError.value = true
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Persists the order so a product can be attached, then hands the id back.
     * Does **not** count as an explicit save.
     */
    fun prepareForAddProduct(onReady: (orderId: Int) -> Unit) {
        if (!canSave || _isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                onReady(persist())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prepare order for product", e)
                _isError.value = true
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Called when leaving without saving. Removes the order only if it was
     * created implicitly to host a product and was never actually used.
     *
     * Uses [appScope] because [viewModelScope] is cancelled as we navigate away.
     */
    fun discardIfAbandoned() {
        if (isDiscarded || wasExplicitlySaved) return
        val id = _orderId.value ?: return

        appScope.launch {
            try {
                val order = repo.getOrder(id)
                // Re-read rather than trusting a flag: the user may have added
                // a product and then backed out without tapping Save.
                if (order != null && order.productList.isEmpty()) {
                    repo.deleteOrder(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to discard abandoned order $id", e)
            }
        }
    }

    // ── Product Actions ────────────────────────
    /**
     * Removes a product from this order. The product is **detached**, not
     * deleted — it may already have been made, and becomes sales-area stock.
     */
    fun removeProductFromOrder(productId: Int) = launchCatching {
        repo.detachProductsByIds(listOf(productId))
    }

    // ── Status Actions ─────────────────────────
    /**
     * Resolves the order: status becomes RESOLVED and every product in it is
     * marked SOLD, atomically. Invokes [onResolved] once the write succeeds.
     */
    fun resolveOrder(onResolved: () -> Unit) {
        val id = _orderId.value ?: return
        if (status.value != OrderStatus.OPEN) return
        if (products.value.isEmpty()) return

        launchCatching {
            repo.resolveOrder(id)
            onResolved()
        }
    }

    /**
     * Cancels the order. Products already made are detached and become
     * sales-area stock, keeping their prices.
     */
    fun cancelOrder(onCancelled: () -> Unit) {
        val id = _orderId.value ?: return
        if (status.value != OrderStatus.OPEN) return

        launchCatching {
            repo.cancelOrder(id)
            onCancelled()
        }
    }

    /**
     * Deletes the order outright. The foreign key is SET_NULL, so its products
     * survive as sales-area stock.
     */
    fun deleteOrder(onDeleted: () -> Unit) {
        val id = _orderId.value
        if (id == null) {
            isDiscarded = true
            onDeleted()
            return
        }

        launchCatching {
            repo.deleteOrder(id)
            isDiscarded = true
            onDeleted()
        }
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
        private const val TAG = "OrderDetailViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            repo: OrderRepository,
            imageStorage: ProductImageStorage,
            appScope: CoroutineScope
        ) = viewModelFactory {
            initializer<OrderDetailViewModel> {
                OrderDetailViewModel(
                    repo = repo,
                    imageStorage = imageStorage,
                    appScope = appScope,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}