package nl.madebypatrick.flipiq.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.repository.CollectionRepository
import nl.madebypatrick.flipiq.domain.model.InventorySummary
import nl.madebypatrick.flipiq.domain.model.Money
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collection: CollectionRepository,
) : ViewModel() {

    val history = collection.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val inventory = collection.inventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary = collection.inventorySummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventorySummary.from(emptyList()))

    fun markSold(id: Long, soldPrice: Money) {
        viewModelScope.launch { runCatching { collection.markSold(id, soldPrice) } }
    }
}
