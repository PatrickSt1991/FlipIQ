package nl.madebypatrick.flipiq.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.madebypatrick.flipiq.domain.model.ThemeMode
import nl.madebypatrick.flipiq.domain.model.ThemePreferences

/** Persists appearance preferences (theme mode + dynamic colour) in the shared DataStore. */
class ThemeRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
    }

    val preferences: Flow<ThemePreferences> = dataStore.data.map { p ->
        ThemePreferences(
            mode = p[Keys.MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemePreferences.DEFAULT.mode,
            dynamicColor = p[Keys.DYNAMIC] ?: ThemePreferences.DEFAULT.dynamicColor,
        )
    }

    suspend fun setMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC] = enabled }
    }
}
