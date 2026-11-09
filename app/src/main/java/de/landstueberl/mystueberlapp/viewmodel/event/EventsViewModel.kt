package de.landstueberl.mystueberlapp.viewmodel.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.EventSections
import de.landstueberl.mystueberlapp.data.splitEvents
import de.landstueberl.mystueberlapp.repository.EventRepository
import de.landstueberl.mystueberlapp.util.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class EventsViewModel(
    repo: EventRepository
) : ViewModel() {

    /**
     * Null while loading, so the screen doesn't flash the empty state.
     * Re-split at midnight, so finished events move to "Past" on their own.
     */
    val sections: StateFlow<EventSections?> =
        combine(repo.getEventsWithProductCountFlow(), todayFlow()) { events, today ->
            splitEvents(events, today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(repo: EventRepository) = viewModelFactory {
            initializer<EventsViewModel> { EventsViewModel(repo) }
        }
    }
}