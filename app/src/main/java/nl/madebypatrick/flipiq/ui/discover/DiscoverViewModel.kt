package nl.madebypatrick.flipiq.ui.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.source.engine.ConsoleGame
import nl.madebypatrick.flipiq.data.source.engine.TopGamesService
import javax.inject.Inject

/** A console the user can browse the most-valuable games for. */
data class Console(val name: String, val slug: String, val emoji: String)

/** Curated set of consoles resellers actually hunt, mapped to PriceCharting slugs. */
val CONSOLES = listOf(
    Console("PlayStation 2", "playstation-2", "🎮"),
    Console("PlayStation 3", "playstation-3", "🎮"),
    Console("PlayStation 1", "playstation", "🎮"),
    Console("Nintendo Wii", "wii", "🕹️"),
    Console("Nintendo Switch", "nintendo-switch", "🔴"),
    Console("GameCube", "gamecube", "🟣"),
    Console("Nintendo 64", "nintendo-64", "🎲"),
    Console("Super Nintendo", "super-nintendo", "🍄"),
    Console("Nintendo DS", "nintendo-ds", "📱"),
    Console("Game Boy Advance", "gameboy-advance", "🔋"),
    Console("Xbox 360", "xbox-360", "🟢"),
    Console("Xbox", "xbox", "🟩"),
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val service: TopGamesService,
) : ViewModel() {

    var selected by mutableStateOf<Console?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var games by mutableStateOf<List<ConsoleGame>>(emptyList())
        private set

    fun select(console: Console) {
        selected = console
        loading = true
        games = emptyList()
        viewModelScope.launch {
            games = service.top(console.slug)
            loading = false
        }
    }

    fun clearSelection() {
        selected = null
        games = emptyList()
        loading = false
    }
}
