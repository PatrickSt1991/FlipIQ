package nl.madebypatrick.flipiq.ui.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import nl.madebypatrick.flipiq.R
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
            TopAppBar(
                title = { Text(selected?.let { "${it.emoji} ${it.name}" } ?: stringResource(R.string.discover_title)) },
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
        items(CONSOLES) { console ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(console) },
                colors = CardDefaults.cardColors(
                    containerColor = Color(console.color),
                    contentColor = Color.White,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(console.emoji, fontSize = 32.sp)
                    Text(
                        console.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameList(games: List<ConsoleGame>, onOpenGame: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(games) { game ->
            ListItem(
                headlineContent = { Text(game.title) },
                trailingContent = {
                    Text(
                        game.value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                modifier = Modifier.clickable { onOpenGame(game.title) },
            )
        }
    }
}
