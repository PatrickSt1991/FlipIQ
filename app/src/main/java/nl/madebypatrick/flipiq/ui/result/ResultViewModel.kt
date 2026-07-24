package nl.madebypatrick.flipiq.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.repository.FetchedMarket
import nl.madebypatrick.flipiq.data.repository.PriceRepository
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val barcode: String = checkNotNull(savedStateHandle[Routes.ARG_BARCODE])

    private var market: FetchedMarket? = null
    private var condition: Condition = Condition.GOOD
    private var completeness: Completeness = Completeness.COMPLETE

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ResultUiState.Loading
        viewModelScope.launch {
            runCatching { repository.fetch(barcode) }
                .onSuccess { market = it; recompute() }
                .onFailure { _state.value = ResultUiState.Error(it.message ?: "Something went wrong.") }
        }
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
            repository.evaluate(current, condition = condition, completeness = completeness),
        )
    }
}
