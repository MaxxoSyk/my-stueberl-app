package de.landstueberl.mystueberlapp.viewmodel.event

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.PriceInput
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.coroutines.cancellation.CancellationException

class EventFormViewModel(
    private val repo: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Int? = savedStateHandle.toRoute<Screen.EventForm>().eventId
    val isNewEvent: Boolean = eventId == null

    /**
     * The event as loaded, so saving keeps the fields this form doesn't show
     * (status, createdAt). Null for a new event.
     */
    private var loadedEvent: EventDetail? = null

    // ── Form Fields ────────────────────────────
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _eventDate = MutableStateFlow<LocalDate?>(null)
    val eventDate: StateFlow<LocalDate?> = _eventDate.asStateFlow()

    private val _endDate = MutableStateFlow<LocalDate?>(null)
    val endDate: StateFlow<LocalDate?> = _endDate.asStateFlow()

    /** Raw text; blank means "not known yet", which is stored as null. */
    private val _standFee = MutableStateFlow("")
    val standFee: StateFlow<String> = _standFee.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    // ── Suggestions ────────────────────────────
    private val _locationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val locationSuggestions: StateFlow<List<String>> = _locationSuggestions.asStateFlow()

    // ── UI State ───────────────────────────────
    private val _isLoading = MutableStateFlow(!isNewEvent)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    // ── Validation ─────────────────────────────
    val isSaveEnabled: StateFlow<Boolean> = combine(_name, _eventDate) { name, date ->
        name.isNotBlank() && date != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    init {
        if (!isNewEvent) loadEvent()
        loadLocationSuggestions()
    }

    // ── Loading ────────────────────────────────
    private fun loadEvent() {
        val id = eventId ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _loadFailed.value = false
            try {
                val event = repo.getEventById(id)
                if (event == null) {
                    _loadFailed.value = true
                } else {
                    loadedEvent = event
                    applyToForm(event)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load event $id", e)
                _loadFailed.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyToForm(event: EventDetail) {
        _name.value = event.name
        _location.value = event.location.orEmpty()
        _eventDate.value = event.eventDate
        _endDate.value = event.endDate
        _standFee.value = event.standFee?.amount?.toPlainString().orEmpty()
        _note.value = event.note.orEmpty()
    }

    fun retry() = loadEvent()

    private fun loadLocationSuggestions() {
        viewModelScope.launch {
            try {
                _locationSuggestions.value = repo.getLocationSuggestions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Suggestions are a convenience; failing silently is acceptable.
                Log.e(TAG, "Failed to load location suggestions", e)
            }
        }
    }

    // ── Field Updates ──────────────────────────
    fun onNameChange(value: String) {
        _name.value = value
    }

    fun onLocationChange(value: String) {
        _location.value = value
    }

    /**
     * Moving the start shifts the end by the same number of days, so a
     * rescheduled multi-day event keeps its length.
     */
    fun onEventDateChange(value: LocalDate) {
        val oldStart = _eventDate.value
        val end = _endDate.value
        if (oldStart != null && end != null) {
            _endDate.value = end.plusDays(ChronoUnit.DAYS.between(oldStart, value))
        }
        _eventDate.value = value
    }

    fun onEndDateChange(value: LocalDate) {
        _endDate.value = value
    }

    /** Turns the event back into a one-day event. */
    fun clearEndDate() {
        _endDate.value = null
    }

    fun onStandFeeChange(value: String) {
        if (PriceInput.isValid(value)) {
            _standFee.value = value
        }
    }

    fun onNoteChange(value: String) {
        _note.value = value
    }

    fun resetErrorState() {
        _isError.value = false
    }

    // ── Save ───────────────────────────────────
    fun saveEvent(onSaved: () -> Unit) {
        val start = _eventDate.value
        if (_name.value.isBlank() || start == null || _isSaving.value) return
        // Editing, but the event never loaded: saving would create a duplicate.
        if (!isNewEvent && loadedEvent == null) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // New events start from the entity defaults (PLANNED, created
                // today); edits start from the loaded row, so fields the form
                // doesn't show (status, createdAt, cancelledOn, …) are kept.
                val base = loadedEvent ?: EventDetail(name = "", eventDate = start)
                repo.saveEvent(
                    base.copy(
                        name = _name.value.trim(),
                        location = _location.value.trim().ifEmpty { null },
                        eventDate = start,
                        // An end on the start day is a one-day event.
                        endDate = _endDate.value?.takeIf { it.isAfter(start) },
                        // parseAmount, not parseMoney: blank or "." stays
                        // "unknown" instead of becoming a free stand.
                        standFee = PriceInput.parseAmount(_standFee.value)
                            ?.let { Money(amount = it, currency = PriceInput.CURRENCY) },
                        note = _note.value.trim().ifEmpty { null }
                    )
                )
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save event", e)
                _isError.value = true
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        private const val TAG = "EventFormViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(repo: EventRepository) = viewModelFactory {
            initializer<EventFormViewModel> {
                EventFormViewModel(
                    repo = repo,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}