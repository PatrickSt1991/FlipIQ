package nl.madebypatrick.flipiq.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import nl.madebypatrick.flipiq.data.repository.CollectionRepository
import nl.madebypatrick.flipiq.domain.stats.FlipStats
import nl.madebypatrick.flipiq.domain.stats.StatsCalculator
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    collection: CollectionRepository,
) : ViewModel() {

    val stats = combine(collection.scanHistory, collection.inventory) { history, inventory ->
        StatsCalculator.compute(history, inventory)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlipStats.EMPTY)
}
