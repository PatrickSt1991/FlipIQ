package nl.madebypatrick.flipiq.ui.collection

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.madebypatrick.flipiq.ui.util.shareCsv
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.InventoryStatus
import nl.madebypatrick.flipiq.domain.model.InventorySummary
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.SavedItem
import nl.madebypatrick.flipiq.domain.model.SavedList
import nl.madebypatrick.flipiq.domain.model.ScanRecord
import nl.madebypatrick.flipiq.ui.result.color
import nl.madebypatrick.flipiq.ui.result.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(0) }
    var sellTarget by remember { mutableStateOf<InventoryItem?>(null) }
    var exportMenu by remember { mutableStateOf(false) }
    val tabs = listOf("Inventory", "Favorites", "Wishlist", "History")
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportMenu = true }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export CSV")
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export inventory (CSV)") },
                            onClick = {
                                exportMenu = false
                                shareCsv(context, "flipiq-inventory.csv", viewModel.inventoryCsv())
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export history (CSV)") },
                            onClick = {
                                exportMenu = false
                                shareCsv(context, "flipiq-history.csv", viewModel.historyCsv())
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ProfitSummaryCard(summary)
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> InventoryList(inventory, onSellClick = { sellTarget = it })
                1 -> SavedItemsList(favorites, "No favorites yet.\nTap the heart on a result to save one.") {
                    viewModel.removeSaved(it.barcode, SavedList.FAVORITE)
                }
                2 -> SavedItemsList(wishlist, "Your wishlist is empty.\nTap the bookmark on a result to add one.") {
                    viewModel.removeSaved(it.barcode, SavedList.WISHLIST)
                }
                else -> HistoryList(history)
            }
        }
    }

    sellTarget?.let { item ->
        SellPriceDialog(
            item = item,
            onConfirm = { price ->
                viewModel.markSold(item.id, price)
                sellTarget = null
            },
            onDismiss = { sellTarget = null },
        )
    }
}

@Composable
private fun ProfitSummaryCard(summary: InventorySummary) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Profit tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SummaryRow("In stock", "${summary.itemsInStock} items · ${summary.capitalInStock}")
            SummaryRow("Sold", "${summary.itemsSold} items")
            SummaryRow("Realized profit", summary.realizedProfit.toString(), emphasise = true)
            SummaryRow("Projected profit", summary.projectedProfit.toString())
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasise: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasise) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun InventoryList(items: List<InventoryItem>, onSellClick: (InventoryItem) -> Unit) {
    if (items.isEmpty()) {
        EmptyState("Nothing in your inventory yet.\nScan an item and tap \"I bought this\".")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    SummaryRow("Bought for", item.buyPrice.toString())
                    if (item.status == InventoryStatus.SOLD) {
                        SummaryRow("Sold for", item.soldPrice?.toString() ?: "—")
                        SummaryRow("Profit", item.realizedProfit?.toString() ?: "—", emphasise = true)
                    } else {
                        SummaryRow("Est. resale", item.estimatedResale.toString())
                        SummaryRow("Projected profit", item.projectedProfit.toString())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onSellClick(item) }) { Text("Mark sold") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedItemsList(
    items: List<SavedItem>,
    emptyMessage: String,
    onRemove: (SavedItem) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(emptyMessage)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.title, fontWeight = FontWeight.Medium, maxLines = 1)
                    IconButton(onClick = { onRemove(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(records: List<ScanRecord>) {
    if (records.isEmpty()) {
        EmptyState("No scans yet.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(records, key = { it.id }) { record ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.padding(end = 12.dp)) {
                        Text(record.title, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            DateUtils.getRelativeTimeSpanString(record.scannedAt).toString(),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${record.dealScore}",
                            fontWeight = FontWeight.Bold,
                            color = record.tier.color,
                        )
                        Text(record.tier.label, style = MaterialTheme.typography.labelSmall, color = record.tier.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SellPriceDialog(
    item: InventoryItem,
    onConfirm: (Money) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("%.2f".format(item.estimatedResale.euros)) }
    val parsed = text.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as sold") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Sold price (€)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(Money.ofEuros(it)) } },
                enabled = parsed != null,
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
