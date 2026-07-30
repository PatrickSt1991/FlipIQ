package nl.madebypatrick.flipiq.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.settings.EbayLocation
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.data.settings.ThemeRepository
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.ThemeMode
import nl.madebypatrick.flipiq.domain.model.ThemePreferences
import javax.inject.Inject

/** One source row for the Settings toggle list — id + display name, no leaking of source objects. */
data class SourceToggle(val id: String, val displayName: String, val enabled: Boolean)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val themeRepository: ThemeRepository,
    sources: List<@JvmSuppressWildcards MarketplaceSource>,
) : ViewModel() {

    // Map id + display name here rather than exposing MarketplaceSource to the UI layer. Built from
    // the injected list so the screen never drifts when a source is added (§7).
    private val sourceIdsAndNames: List<Pair<String, String>> = sources.map { it.id to it.displayName }

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfitSettings.DEFAULT)

    val sourceToggles = settingsRepository.disabledSourceIds
        .map { disabled -> sourceIdsAndNames.map { (id, name) -> SourceToggle(id, name, id !in disabled) } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            sourceIdsAndNames.map { (id, name) -> SourceToggle(id, name, true) },
        )

    fun setSourceEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { runCatching { settingsRepository.setSourceEnabled(id, enabled) } }
    }

    val theme = themeRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreferences.DEFAULT)

    val ebayLocation = settingsRepository.ebayLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EbayLocation.DEFAULT)

    fun setEbayLocation(location: EbayLocation) {
        viewModelScope.launch { runCatching { settingsRepository.setEbayLocation(location) } }
    }


    val eanSearchToken = settingsRepository.eanSearchToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun save(settings: ProfitSettings) {
        viewModelScope.launch { runCatching { settingsRepository.update(settings) } }
    }


    fun setEanSearchToken(token: String) {
        viewModelScope.launch { runCatching { settingsRepository.setEanSearchToken(token) } }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { runCatching { themeRepository.setMode(mode) } }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { runCatching { themeRepository.setDynamicColor(enabled) } }
    }
}
