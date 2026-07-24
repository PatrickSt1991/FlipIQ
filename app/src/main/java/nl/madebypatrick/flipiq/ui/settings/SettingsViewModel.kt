package nl.madebypatrick.flipiq.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.data.settings.ThemeRepository
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.ThemeMode
import nl.madebypatrick.flipiq.domain.model.ThemePreferences
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfitSettings.DEFAULT)

    val theme = themeRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreferences.DEFAULT)

    fun save(settings: ProfitSettings) {
        viewModelScope.launch { runCatching { settingsRepository.update(settings) } }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { runCatching { themeRepository.setMode(mode) } }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { runCatching { themeRepository.setDynamicColor(enabled) } }
    }
}
