package de.landstueberl.mystueberlapp.viewmodel.event

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.landstueberl.mystueberlapp.data.EventStatus
import de.landstueberl.mystueberlapp.data.db.entity.EventDetail
import de.landstueberl.mystueberlapp.navigation.Screen
import de.landstueberl.mystueberlapp.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data object NotFound : EventDetailUiState
    data class Loaded(val event: EventDetail) : EventDetailUiState
}

class EventDetailViewModel(
    private val repo: EventRepository,
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

    // ── UI State ───────────────────────────────
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun resetErrorState() {
        _isError.value = false
    }

    // ── Actions ────────────────────────────────
    /** PLANNED → IN_PREPARATION. Other transitions follow with their screens. */
    fun startPreparation() {
        val event = (uiState.value as? EventDetailUiState.Loaded)?.event ?: return
        if (event.status != EventStatus.PLANNED) return

        launchWorking { repo.updateStatus(eventId, EventStatus.IN_PREPARATION) }
    }

    /** Product links are removed by CASCADE; the products themselves stay. */
    fun deleteEvent(onDeleted: () -> Unit) = launchWorking {
        isDeleting.value = true
        try {
            repo.deleteEvent(eventId)
        } catch (e: Exception) {
            isDeleting.value = false
            throw e
        }
        onDeleted()
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

        fun factory(repo: EventRepository) = viewModelFactory {
            initializer<EventDetailViewModel> {
                EventDetailViewModel(
                    repo = repo,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}