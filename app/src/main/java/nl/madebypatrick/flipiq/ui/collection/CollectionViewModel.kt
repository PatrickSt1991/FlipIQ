package nl.madebypatrick.flipiq.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.repository.AlertRepository
import nl.madebypatrick.flipiq.data.repository.CollectionRepository
import nl.madebypatrick.flipiq.domain.export.CsvFormatter
import nl.madebypatrick.flipiq.domain.model.InventorySummary
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.SavedList
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collection: CollectionRepository,
    private val alertRepository: AlertRepository,
) : ViewModel() {

    val history = collection.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val inventory = collection.inventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary = collection.inventorySummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventorySummary.from(emptyList()))

    val favorites = collection.savedItems(SavedList.FAVORITE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wishlist = collection.savedItems(SavedList.WISHLIST)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val alerts = alertRepository.alerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markSold(id: Long, soldPrice: Money) {
        viewModelScope.launch { runCatching { collection.markSold(id, soldPrice) } }
    }

    /** Edit an existing item — title, category and both prices. */
    fun updateItem(id: Long, title: String, category: String?, buyPrice: Money, estimatedResale: Money) {
        viewModelScope.launch {
            runCatching { collection.updateInventory(id, title, category, buyPrice, estimatedResale) }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { runCatching { collection.deleteInventory(id) } }
    }

    /** Add an item to inventory by hand (no scan) — a title, what you paid, and expected resale. */
    fun addManual(title: String, buyPrice: Money, estimatedResale: Money) {
        viewModelScope.launch {
            runCatching {
                collection.addToInventory(
                    barcode = "manual",
                    title = title,
                    buyPrice = buyPrice,
                    estimatedResale = estimatedResale,
                )
            }
        }
    }

    fun removeSaved(barcode: String, list: SavedList) {
        viewModelScope.launch { runCatching { collection.setSaved(barcode, "", list, saved = false) } }
    }

    fun removeAlert(id: Long) {
        viewModelScope.launch { runCatching { alertRepository.delete(id) } }
    }

    fun inventoryCsv(): String = CsvFormatter.inventoryCsv(inventory.value)

    fun historyCsv(): String = CsvFormatter.historyCsv(history.value)
}
