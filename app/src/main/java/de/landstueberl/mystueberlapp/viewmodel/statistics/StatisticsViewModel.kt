package de.landstueberl.mystueberlapp.viewmodel.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.landstueberl.mystueberlapp.data.OverallFigures
import de.landstueberl.mystueberlapp.data.PeriodFigures
import de.landstueberl.mystueberlapp.data.StatisticsPeriod
import de.landstueberl.mystueberlapp.data.rangeOf
import de.landstueberl.mystueberlapp.repository.StatisticsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val repo: StatisticsRepository
) : ViewModel() {

    private val _period = MutableStateFlow(StatisticsPeriod.TODAY)
    val period: StateFlow<StatisticsPeriod> = _period.asStateFlow()

    fun setPeriod(period: StatisticsPeriod) {
        _period.value = period
    }

    /** Re-queries when the period changes. */
    val periodFigures: StateFlow<PeriodFigures?> = _period
        .flatMapLatest { period ->
            repo.getPeriodFiguresFlow(period.rangeOf())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val overallFigures: StateFlow<OverallFigures?> = repo
        .getOverallFiguresFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(repo: StatisticsRepository) = viewModelFactory {
            initializer<StatisticsViewModel> { StatisticsViewModel(repo) }
        }
    }
}