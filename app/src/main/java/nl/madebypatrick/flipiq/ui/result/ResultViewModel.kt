package nl.madebypatrick.flipiq.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.repository.AlertRepository
import nl.madebypatrick.flipiq.data.repository.CollectionRepository
import nl.madebypatrick.flipiq.data.repository.FetchedMarket
import nl.madebypatrick.flipiq.data.repository.PriceRepository
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.SavedList
import nl.madebypatrick.flipiq.domain.model.ScanAnalysis
import nl.madebypatrick.flipiq.ui.Routes
import javax.inject.Inject

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data class Error(val message: String) : ResultUiState
    data class Success(val analysis: ScanAnalysis) : ResultUiState
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: PriceRepository,
    private val collection: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val alertRepository: AlertRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val barcode: String = checkNotNull(savedStateHandle[Routes.ARG_BARCODE])

    private var market: FetchedMarket? = null
    private var condition: Condition = Condition.GOOD
    private var completeness: Completeness = Completeness.COMPLETE
    private var settings: ProfitSettings = ProfitSettings.DEFAULT
    private var recorded = false

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val state = _state.asStateFlow()

    /** One-shot user-facing messages (e.g. "Added to inventory"); cleared once shown. */
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val isFavorite = collection.isSaved(barcode, SavedList.FAVORITE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val isWishlisted = collection.isSaved(barcode, SavedList.WISHLIST)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val hasAlert = alertRepository.hasActiveAlert(barcode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        load()
    }

    fun setPriceAlert(target: Money) {
        viewModelScope.launch {
            runCatching { alertRepository.create(barcode, currentTitle(), target) }
                .onSuccess { _message.value = "Price alert set for $target" }
                .onFailure { _message.value = "Couldn't set the alert" }
        }
    }

    private fun currentTitle(): String =
        (_state.value as? ResultUiState.Success)?.analysis?.product?.title ?: barcode

    fun toggleFavorite() {
        val target = !isFavorite.value
        viewModelScope.launch {
            runCatching { collection.setSaved(barcode, currentTitle(), SavedList.FAVORITE, target) }
        }
    }

    fun toggleWishlist() {
        val target = !isWishlisted.value
        viewModelScope.launch {
            runCatching { collection.setSaved(barcode, currentTitle(), SavedList.WISHLIST, target) }
        }
    }

    fun load() {
        _state.value = ResultUiState.Loading
        viewModelScope.launch {
            settings = runCatching { settingsRepository.settings.first() }.getOrDefault(ProfitSettings.DEFAULT)
            runCatching { repository.fetch(barcode) }
                .onSuccess { market = it; recompute(); recordOnce() }
                .onFailure { _state.value = ResultUiState.Error(it.message ?: "Something went wrong.") }
        }
    }

    /** Save this scan to history exactly once per screen. */
    private fun recordOnce() {
        if (recorded) return
        val analysis = (_state.value as? ResultUiState.Success)?.analysis ?: return
        recorded = true
        viewModelScope.launch { runCatching { collection.recordScan(analysis) } }
    }

    /** Add the scanned item to inventory at the given buy price (defaults to the recommended max). */
    fun markAsBought(buyPrice: Money? = null) {
        val analysis = (_state.value as? ResultUiState.Success)?.analysis ?: return
        val rec = analysis.recommendation
        viewModelScope.launch {
            runCatching {
                collection.addToInventory(
                    barcode = analysis.product.barcode,
                    title = analysis.product.title,
                    buyPrice = buyPrice ?: rec.recommendedBuyPrice,
                    estimatedResale = rec.estimatedResale,
                )
            }.onSuccess { _message.value = "Added to inventory" }
                .onFailure { _message.value = "Couldn't add to inventory" }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    fun setCondition(value: Condition) {
        condition = value
        recompute()
    }

    fun setCompleteness(value: Completeness) {
        completeness = value
        recompute()
    }

    private fun recompute() {
        val current = market ?: return
        _state.value = ResultUiState.Success(
            repository.evaluate(current, condition = condition, completeness = completeness, settings = settings),
        )
    }
}
