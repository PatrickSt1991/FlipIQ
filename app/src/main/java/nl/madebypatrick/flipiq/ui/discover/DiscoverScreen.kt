package nl.madebypatrick.flipiq.ui.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.ui.components.ValooTopBar
import nl.madebypatrick.flipiq.data.source.engine.ConsoleGame

/**
 * "What to hunt for" — pick a console, see its most valuable games (from the engine's `/top`), tap
 * one to price it. A buying-side complement to scanning: know what's worth grabbing before you do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val selected = viewModel.selected

    // Within a console list, Back returns to the console grid rather than leaving the screen.
    BackHandler(enabled = selected != null) { viewModel.clearSelection() }

    Scaffold(
        topBar = {
            ValooTopBar(
                title = selected?.let { "${it.emoji} ${it.name}" } ?: stringResource(R.string.discover_title),
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) viewModel.clearSelection() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selected == null) {
                ConsoleGrid(onSelect = viewModel::select)
            } else if (viewModel.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.games.isEmpty()) {
                Text(
                    stringResource(R.string.discover_load_error, selected.name),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                GameList(games = viewModel.games, onOpenGame = onOpenGame)
            }
        }
    }
}

@Composable
private fun ConsoleGrid(onSelect: (Console) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(
                    stringResource(R.string.discover_headline),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.discover_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(CONSOLES) { console ->
            ConsoleTile(console = console, onSelect = onSelect)
        }
    }
}

@Composable
private fun ConsoleTile(console: Console, onSelect: (Console) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(console) },
        colors = CardDefaults.cardColors(
            containerColor = Color(console.color),
            contentColor = Color.White,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Emoji sits in a soft translucent disc so every tile reads consistently.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(console.emoji, fontSize = 22.sp)
            }
            Text(
                console.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GameList(games: List<ConsoleGame>, onOpenGame: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(games) { index, game ->
            GameRow(rank = index + 1, game = game, onOpenGame = onOpenGame)
        }
    }
}

// Medal tints for the top three; the rest get the brand container.
private val GOLD = Color(0xFFFFC107)
private val SILVER = Color(0xFFB0BEC5)
private val BRONZE = Color(0xFFCD7F32)

@Composable
private fun GameRow(rank: Int, game: ConsoleGame, onOpenGame: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenGame(game.title) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val medal = when (rank) {
                1 -> GOLD; 2 -> SILVER; 3 -> BRONZE
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            val onMedal = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(medal),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", color = onMedal, fontWeight = FontWeight.Bold)
            }
            Text(
                game.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    game.value.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
