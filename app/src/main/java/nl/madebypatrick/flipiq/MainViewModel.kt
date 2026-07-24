package nl.madebypatrick.flipiq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import nl.madebypatrick.flipiq.data.settings.ThemeRepository
import nl.madebypatrick.flipiq.domain.model.ThemePreferences
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    themeRepository: ThemeRepository,
) : ViewModel() {

    val theme = themeRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreferences.DEFAULT)
}
