package nl.madebypatrick.flipiq.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.stats.FlipStats
import nl.madebypatrick.flipiq.ui.result.color
import nl.madebypatrick.flipiq.ui.result.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (stats.totalScans == 0 && stats.inventory.itemsInStock == 0 && stats.inventory.itemsSold == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Scan and buy some items to see your stats.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeadlineTiles(stats)
            TierBreakdownCard(stats)
            if (stats.topFlips.isNotEmpty()) TopFlipsCard(stats.topFlips)
        }
    }
}

@Composable
private fun HeadlineTiles(stats: FlipStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("Scans", stats.totalScans.toString(), Modifier.weight(1f))
        StatTile("Avg score", stats.averageDealScore.toString(), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("Realized profit", stats.inventory.realizedProfit.toString(), Modifier.weight(1f))
        StatTile("Projected profit", stats.inventory.projectedProfit.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TierBreakdownCard(stats: FlipStats) {
    val max = (stats.tierBreakdown.values.maxOrNull() ?: 0).coerceAtLeast(1)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Deal scores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DealTier.entries.forEach { tier ->
                val count = stats.tierBreakdown[tier] ?: 0
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(tier.label, style = MaterialTheme.typography.bodyMedium, color = tier.color)
                        Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(count.toFloat() / max)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(tier.color),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopFlipsCard(topFlips: List<InventoryItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Top flips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            topFlips.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(
                        item.projectedProfit.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
