package nl.madebypatrick.flipiq.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfitSettings.DEFAULT)

    fun save(settings: ProfitSettings) {
        viewModelScope.launch { runCatching { settingsRepository.update(settings) } }
    }
}
