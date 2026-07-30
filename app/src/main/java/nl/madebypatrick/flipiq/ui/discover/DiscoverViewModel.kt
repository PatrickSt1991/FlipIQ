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

/** A console the user can browse the most-valuable games for. [color] is its brand tint (ARGB). */
data class Console(val name: String, val slug: String, val emoji: String, val color: Long)

// Brand tints so the tiles read as PlayStation / Nintendo / Xbox at a glance instead of identical
// grey cards with the same 🎮.
private const val PLAYSTATION = 0xFF003791
private const val NINTENDO = 0xFFE60012
private const val XBOX = 0xFF107C10

/** Curated set of consoles resellers actually hunt. The slug maps to an eBay.nl query in the engine. */
val CONSOLES = listOf(
    Console("PlayStation 2", "playstation-2", "🎮", PLAYSTATION),
    Console("PlayStation 3", "playstation-3", "🎮", PLAYSTATION),
    Console("PlayStation 1", "playstation", "🎮", PLAYSTATION),
    Console("Nintendo Wii", "wii", "🕹️", NINTENDO),
    Console("Nintendo Switch", "nintendo-switch", "🔴", NINTENDO),
    Console("GameCube", "gamecube", "🟣", NINTENDO),
    Console("Nintendo 64", "nintendo-64", "🎲", NINTENDO),
    Console("Super Nintendo", "super-nintendo", "🍄", NINTENDO),
    Console("Nintendo DS", "nintendo-ds", "📱", NINTENDO),
    Console("Game Boy Advance", "gameboy-advance", "🔋", NINTENDO),
    Console("Xbox 360", "xbox-360", "🎮", XBOX),
    Console("Xbox", "xbox", "🎮", XBOX),
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
