package de.landstueberl.mystueberlapp.viewmodel.event

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.data.db.entity.EventSale
import de.landstueberl.mystueberlapp.data.db.relation.Product
import de.landstueberl.mystueberlapp.data.image.ProductImageStorage
import de.landstueberl.mystueberlapp.data.label.BatchLabelCode
import de.landstueberl.mystueberlapp.data.label.ProductLabel
import de.landstueberl.mystueberlapp.data.label.ProductLabelCode
import de.landstueberl.mystueberlapp.data.model.event.EventProducts
import de.landstueberl.mystueberlapp.data.model.event.EventStatus
import de.landstueberl.mystueberlapp.data.model.event.toEventProducts
import de.landstueberl.mystueberlapp.data.model.product.ProductListItem
import de.landstueberl.mystueberlapp.data.model.product.allProducts
import de.landstueberl.mystueberlapp.data.model.product.groupByBatch
import de.landstueberl.mystueberlapp.data.model.till.TillFigures
import de.landstueberl.mystueberlapp.data.model.till.toTillFigures
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.repository.EventSaleRepository
import de.landstueberl.mystueberlapp.viewmodel.common.AttachSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data object NotFound : EventDetailUiState
    data class Loaded(val event: EventDetail) : EventDetailUiState
}

/** Specific reasons an action was refused, shown as a snackbar. */
enum class EventNotice { CLOSE_BLOCKED, REOPEN_REFUSED, DELETE_BLOCKED }

/** A print job waiting for "Did the labels print correctly?". */
data class PendingLabelPrint(
    val productIds: List<Int>,
    /** batchId → identifier printed on the group label. */
    val batchIdentifiers: Map<Int, String>
) {
    val labelCount: Int get() = productIds.size + batchIdentifiers.size
}

/** The sheet to print, and what to save once it's confirmed. */
data class PreparedLabels(
    val labels: List<ProductLabel>,
    val pending: PendingLabelPrint
)

class EventDetailViewModel(
    private val repo: EventRepository,
    private val saleRepo: EventSaleRepository,
    private val imageStorage: ProductImageStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Int = savedStateHandle.toRoute<Screen.EventDetail>().eventId

    /** Set while deleting, so the screen doesn't flash "not found" on the way out. */
    private val isDeleting = MutableStateFlow(false)

    /** Observed live, so status changes and edits show up immediately. */
    val uiState: StateFlow<EventDetailUiState> =
        combine(repo.getEventFlow(eventId), isDeleting) { event, deleting ->
            when {
                deleting -> EventDetailUiState.Loading
                event == null -> EventDetailUiState.NotFound
                else -> EventDetailUiState.Loaded(event)
            }
        }
            .catch { e ->
                Log.e(TAG, "Failed to observe event $eventId", e)
                emit(EventDetailUiState.NotFound)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                EventDetailUiState.Loading
            )

    /**
     * The event's products, observed live: linking, unlinking, selling at the
     * till and selling elsewhere all show up immediately. Null while loading.
     */
    val products: StateFlow<EventProducts?> = combine(
        repo.getEventProductsFlow(eventId),
        saleRepo.getSalesFlow(eventId).map { sales -> sales.mapTo(HashSet()) { it.id } },
        // While preparing, the packing list grows from the top.
        repo.getEventFlow(eventId)
            .map { it?.status == EventStatus.IN_PREPARATION }
            .distinctUntilChanged()
    ) { list, saleIds, newestFirst ->
        list.toEventProducts(saleIds, newestFirst)
    }
        .catch { e ->
            Log.e(TAG, "Failed to observe products of event $eventId", e)
            emit(emptyList<Product>().toEventProducts())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    // ── Attach Existing Products ───────────────
    /** Stand stock that can be taken to this event, grouped like the products list. */
    val attachableItems: StateFlow<List<ProductListItem>> =
        repo.getAttachableProductsFlow(eventId)
            .map { it.groupByBatch() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val attachSelection = AttachSelection()
    val selectedForAttach: StateFlow<Set<Int>> = attachSelection.selectedIds

    fun toggleAttachSelection(productId: Int) = attachSelection.toggle(productId)

    fun toggleBatchAttachSelection(productIds: List<Int>) = attachSelection.toggleBatch(productIds)

    fun clearAttachSelection() = attachSelection.clear()

    /**
     * Links the ticked products. The SQL guard skips any that were sold or
     * assigned elsewhere meanwhile, and links nothing unless still preparing.
     */
    fun attachSelectedProducts(onAttached: () -> Unit) {
        val ids = attachSelection.current.toList()
        if (ids.isEmpty() || currentEvent?.status != EventStatus.IN_PREPARATION) return

        launchWorking {
            repo.linkProducts(eventId, ids)
            attachSelection.clear()
            onAttached()
        }
    }

    private val currentEvent: EventDetail?
        get() = (uiState.value as? EventDetailUiState.Loaded)?.event

    // ── Label Printing ─────────────────────────
    /** Separate from the attach selection, so the two pickers never share ticks. */
    private val labelSelection = AttachSelection()
    val selectedForLabels: StateFlow<Set<Int>> = labelSelection.selectedIds

    fun toggleLabelSelection(productId: Int) = labelSelection.toggle(productId)

    fun toggleBatchLabelSelection(productIds: List<Int>) = labelSelection.toggleBatch(productIds)

    fun clearLabelSelection() {
        labelSelection.clear()
        _selectedGroups.value = emptySet()
    }

    /** Ticks every product label and every group label in the open dialog. */
    fun selectAllForLabels() {
        val groups = _groupBatches.value
        labelSelection.replaceWith(
            availableProducts().filter { it.details.batchId !in groups }.map { it.details.id }
        )
        _selectedGroups.value = groups
    }

    /** Labels for the ticked products and groups, plus what to save on confirm. */
    suspend fun prepareLabels(): PreparedLabels {
        val available = availableProducts()
        val groups = _groupBatches.value

        val productsToPrint = available.filter {
            it.details.id in labelSelection.current && it.details.batchId !in groups
        }
        // New identifiers for products that lost theirs to a group label.
        repo.ensureLabelIdentifiers(eventId, productsToPrint.map { it.details.id })
        // Each product's own identifier, read fresh: a group label that is being
        // replaced must not end up in the caption.
        val ownIdentifiers = repo.getLabelIdentifiersFlow(eventId).first()

        val productLabels = productsToPrint.map { product ->
            ProductLabel(
                code = ProductLabelCode(eventId, product.details.id),
                caption = caption(ownIdentifiers[product.details.id], product.details.description)
            )
        }

        val batchIds = _selectedGroups.value.filter { it in groups }
        val batchIdentifiers = repo.prepareBatchIdentifiers(eventId, batchIds)
        val groupLabels = batchIds.mapNotNull { batchId ->
            val members = available.filter { it.details.batchId == batchId }
            val first = members.firstOrNull() ?: return@mapNotNull null
            ProductLabel(
                code = BatchLabelCode(eventId, batchId),
                caption = caption(batchIdentifiers[batchId], first.details.description) +
                        " ×${members.size}"
            )
        }

        return PreparedLabels(
            labels = productLabels + groupLabels,
            pending = PendingLabelPrint(
                productIds = productLabels.map { (it.code as ProductLabelCode).productId },
                batchIdentifiers = batchIdentifiers.filterKeys { id -> groupLabels.any { (it.code as BatchLabelCode).batchId == id } }
            )
        )
    }

    /** Identifier first, so ellipsizing on the sheet only shortens the description. */
    private fun caption(identifier: String?, description: String): String =
        if (identifier == null) description else "$identifier · $description"

    /** Observed live, so the dialog shows "Printed on …" right after confirming. */
    val labelPrintedOn: StateFlow<Map<Int, LocalDate>> = repo.getLabelPrintDatesFlow(eventId)
        .catch { e ->
            Log.e(TAG, "Failed to observe label prints of event $eventId", e)
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    /** batchId → printed date of the batch's group label; covers all its members. */
    val batchLabelPrintedOn: StateFlow<Map<Int, LocalDate>> = repo.getBatchLabelDatesFlow(eventId)
        .catch { e ->
            Log.e(TAG, "Failed to observe group labels of event $eventId", e)
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    private val labelIdentifiers: StateFlow<Map<Int, String>> = repo.getLabelIdentifiersFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    private val batchLabelIdentifiers: StateFlow<Map<Int, String>> =
        repo.getBatchLabelIdentifiersFlow(eventId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    private val _pendingPrint = MutableStateFlow<PendingLabelPrint?>(null)
    val pendingPrint: StateFlow<PendingLabelPrint?> = _pendingPrint.asStateFlow()

    fun onPrintStarted(pending: PendingLabelPrint) {
        _pendingPrint.value = pending
    }

    /** "Yes, mark as printed": saves exactly what was on the sheet. */
    fun confirmPendingPrint() {
        val pending = _pendingPrint.value ?: return
        _pendingPrint.value = null
        launchWorking {
            repo.markLabelsPrinted(eventId, pending.productIds)
            repo.markBatchLabelsPrinted(eventId, pending.batchIdentifiers)
        }
    }

    fun dismissPendingPrint() {
        _pendingPrint.value = null
    }

    /**
     * productId → the identifier shown for it, only once its label is printed:
     * the batch's while it has a printed group label (that identifier is only
     * saved on confirm), otherwise the product's own if its label was printed.
     */
    val effectiveIdentifiers: StateFlow<Map<Int, String>> =
        combine(products, labelIdentifiers, batchLabelIdentifiers, labelPrintedOn) { list, own, batch, printed ->
            buildMap {
                list?.items.orEmpty().allProducts().forEach { product ->
                    val id = product.details.id
                    val identifier = product.details.batchId?.let(batch::get)
                        ?: own[id]?.takeIf { id in printed }
                    identifier?.let { put(id, it) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    /** Used when the dialog opens: only products without their own or a group label. */
    private fun preselectUnprintedLabels() {
        val printed = labelPrintedOn.value
        val batchLabels = batchLabelPrintedOn.value
        labelSelection.replaceWith(
            availableProducts()
                .filterNot { hasLabel(it, printed, batchLabels) }
                .map { it.details.id }
        )
    }

    /** Batches printed as one group label in the open dialog. */
    private val _groupBatches = MutableStateFlow<Set<Int>>(emptySet())
    val groupBatches: StateFlow<Set<Int>> = _groupBatches.asStateFlow()

    /** Group labels ticked for printing (batchIds). */
    private val _selectedGroups = MutableStateFlow<Set<Int>>(emptySet())
    val selectedGroups: StateFlow<Set<Int>> = _selectedGroups.asStateFlow()

    /** When the dialog opens: per product by default, group where a group label exists. */
    fun openLabelSelection() {
        val printedGroups = batchLabelPrintedOn.value.keys
        val batchesInList = availableProducts().mapNotNullTo(HashSet()) { it.details.batchId }
        _groupBatches.value = printedGroups intersect batchesInList
        _selectedGroups.value = emptySet() // printed group labels aren't preselected
        preselectUnprintedLabels()
    }

    /** Switches one batch between "one group label" and "one label per product". */
    fun setGroupMode(batchId: Int, group: Boolean) {
        val members = availableProducts().filter { it.details.batchId == batchId }
        if (group) {
            _groupBatches.update { it + batchId }
            members.forEach { labelSelection.deselect(it.details.id) }
            if (batchId !in batchLabelPrintedOn.value) _selectedGroups.update { it + batchId }
        } else {
            _groupBatches.update { it - batchId }
            _selectedGroups.update { it - batchId }
            // Members without their own label are preselected.
            members.filter { it.details.id !in labelPrintedOn.value }
                .forEach { labelSelection.select(it.details.id) }
        }
    }

    fun toggleGroupSelection(batchId: Int) =
        _selectedGroups.update { if (batchId in it) it - batchId else it + batchId }

    private fun availableProducts(): List<Product> =
        products.value?.items.orEmpty().allProducts()

    // ── UI State ───────────────────────────────
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun resetErrorState() {
        _isError.value = false
    }

    fun imageFile(fileName: String): File = imageStorage.fileFor(fileName)

    // ── Actions ────────────────────────────────
    /** PLANNED → IN_PREPARATION. Other transitions follow with their screens. */
    fun startPreparation() {
        if (currentEvent?.status != EventStatus.PLANNED) return
        launchWorking { repo.updateStatus(eventId, EventStatus.IN_PREPARATION) }
    }

    /** IN_PREPARATION → IN_PROGRESS. Needs at least one available product (guarded in SQL too). */
    fun startEvent() {
        if (currentEvent?.status != EventStatus.IN_PREPARATION) return
        if ((products.value?.availableCount ?: 0) == 0) return

        launchWorking {
            // A failed guard (e.g. the last product was sold meanwhile) shows the error snackbar.
            check(repo.startEvent(eventId)) { "Event $eventId could not be started" }
        }
    }

    /** IN_PROGRESS → DONE. Refused in SQL while the basket isn't empty. */
    fun closeEvent() {
        if (currentEvent?.status != EventStatus.IN_PROGRESS) return
        launchWorking {
            if (!repo.closeEvent(eventId)) _notice.value = EventNotice.CLOSE_BLOCKED
        }
    }

    /**
     * DONE → IN_PROGRESS while the last day hasn't passed. Refused in SQL if a
     * product was put into an order or another event meanwhile.
     */
    fun reopenEvent() {
        val event = currentEvent ?: return
        if (!event.canReopen(LocalDate.now())) return
        launchWorking {
            if (!repo.reopenEvent(eventId)) _notice.value = EventNotice.REOPEN_REFUSED
        }
    }

    /** Links stay as history; the products are free again. Guarded in SQL as well. */
    fun cancelEvent() {
        if (currentEvent?.status?.isCancellable != true) return
        launchWorking { repo.cancelEvent(eventId) }
    }

    /** Unlinks products; they stay in stock. Only while preparing (guarded in SQL too). */
    fun removeProducts(productIds: List<Int>) {
        if (currentEvent?.status != EventStatus.IN_PREPARATION) return
        launchWorking { repo.unlinkProducts(eventId, productIds) }
    }

    /**
     * Product links are removed by CASCADE; the products themselves stay.
     * Refused in SQL if the event has sales, e.g. one completed meanwhile.
     */
    fun deleteEvent(onDeleted: () -> Unit) = launchWorking {
        isDeleting.value = true
        val deleted = try {
            repo.deleteEvent(eventId)
        } catch (e: Exception) {
            isDeleting.value = false
            throw e
        }
        if (deleted) {
            onDeleted()
        } else {
            // Back to the screen instead of the spinner, with the reason.
            isDeleting.value = false
            _notice.value = EventNotice.DELETE_BLOCKED
        }
    }

    // ── Closing ────────────────────────────────
    /** Closing is blocked while products are still in the till's basket. */
    val basketCount: StateFlow<Int> = saleRepo.getBasketCountFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    /** An event with sales can't be deleted (guarded in SQL too). */
    val hasSales: StateFlow<Boolean> = saleRepo.hasSalesFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun notifyDeleteBlocked() {
        _notice.value = EventNotice.DELETE_BLOCKED
    }

    /** Revenue, items and customers for the close summary. */
    val figures: StateFlow<TillFigures> = saleRepo.getSalesFlow(eventId)
        .map { it.toTillFigures() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            emptyList<EventSale>().toTillFigures()
        )

    private val _notice = MutableStateFlow<EventNotice?>(null)
    val notice: StateFlow<EventNotice?> = _notice.asStateFlow()

    fun resetNotice() {
        _notice.value = null
    }

    private fun launchWorking(block: suspend () -> Unit) {
        if (_isWorking.value) return

        viewModelScope.launch {
            _isWorking.value = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Event action failed", e)
                _isError.value = true
            } finally {
                _isWorking.value = false
            }
        }
    }

    companion object {
        private const val TAG = "EventDetailViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            repo: EventRepository,
            saleRepo: EventSaleRepository,
            imageStorage: ProductImageStorage
        ) = viewModelFactory {
            initializer<EventDetailViewModel> {
                EventDetailViewModel(
                    repo = repo,
                    saleRepo = saleRepo,
                    imageStorage = imageStorage,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}

/**
 * A product has a label if its own label was printed, or its batch has a
 * printed group label at this event (which also covers members added later).
 */
fun hasLabel(
    product: Product,
    printedOn: Map<Int, LocalDate>,
    batchLabelPrintedOn: Map<Int, LocalDate>
): Boolean =
    product.details.id in printedOn ||
            product.details.batchId?.let { it in batchLabelPrintedOn } == true