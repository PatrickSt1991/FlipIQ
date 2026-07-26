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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.ui.util.shareCsv
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.InventoryStatus
import nl.madebypatrick.flipiq.domain.model.InventorySummary
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.PriceAlert
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
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(0) }
    var sellTarget by remember { mutableStateOf<InventoryItem?>(null) }
    var exportMenu by remember { mutableStateOf(false) }
    val tabs = listOf(
        stringResource(R.string.collection_tab_inventory),
        stringResource(R.string.collection_tab_favorites),
        stringResource(R.string.collection_tab_wishlist),
        stringResource(R.string.collection_tab_alerts),
        stringResource(R.string.collection_tab_history),
    )
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collection_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { exportMenu = true }) {
                        Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.collection_cd_export))
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.collection_export_inventory)) },
                            onClick = {
                                exportMenu = false
                                shareCsv(context, "flipiq-inventory.csv", viewModel.inventoryCsv())
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.collection_export_history)) },
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
                1 -> SavedItemsList(favorites, stringResource(R.string.collection_empty_favorites)) {
                    viewModel.removeSaved(it.barcode, SavedList.FAVORITE)
                }
                2 -> SavedItemsList(wishlist, stringResource(R.string.collection_empty_wishlist)) {
                    viewModel.removeSaved(it.barcode, SavedList.WISHLIST)
                }
                3 -> AlertsList(alerts) { viewModel.removeAlert(it.id) }
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
            Text(stringResource(R.string.collection_profit_tracker), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SummaryRow(stringResource(R.string.collection_in_stock), stringResource(R.string.collection_in_stock_value, summary.itemsInStock, summary.capitalInStock.toString()))
            SummaryRow(stringResource(R.string.collection_sold), stringResource(R.string.collection_sold_value, summary.itemsSold))
            SummaryRow(stringResource(R.string.collection_realized_profit), summary.realizedProfit.toString(), emphasise = true)
            SummaryRow(stringResource(R.string.collection_projected_profit), summary.projectedProfit.toString())
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
        EmptyState(stringResource(R.string.collection_empty_inventory))
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
                    SummaryRow(stringResource(R.string.collection_bought_for), item.buyPrice.toString())
                    if (item.status == InventoryStatus.SOLD) {
                        SummaryRow(stringResource(R.string.collection_sold_for), item.soldPrice?.toString() ?: "—")
                        SummaryRow(stringResource(R.string.collection_profit), item.realizedProfit?.toString() ?: "—", emphasise = true)
                    } else {
                        SummaryRow(stringResource(R.string.collection_est_resale), item.estimatedResale.toString())
                        SummaryRow(stringResource(R.string.collection_projected_profit), item.projectedProfit.toString())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onSellClick(item) }) { Text(stringResource(R.string.collection_mark_sold)) }
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
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.collection_cd_remove))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertsList(alerts: List<PriceAlert>, onRemove: (PriceAlert) -> Unit) {
    if (alerts.isEmpty()) {
        EmptyState(stringResource(R.string.collection_empty_alerts))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(alerts, key = { it.id }) { alert ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(alert.title, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(
                            stringResource(R.string.collection_notify_at, alert.targetPrice.toString()),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    IconButton(onClick = { onRemove(alert) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.collection_cd_remove_alert))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(records: List<ScanRecord>) {
    if (records.isEmpty()) {
        EmptyState(stringResource(R.string.collection_empty_history))
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
        title = { Text(stringResource(R.string.collection_mark_sold_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.collection_sold_price_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(Money.ofEuros(it)) } },
                enabled = parsed != null,
            ) { Text(stringResource(R.string.collection_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
