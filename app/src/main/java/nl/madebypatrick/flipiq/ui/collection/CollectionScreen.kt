package nl.madebypatrick.flipiq.ui.collection

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.ui.components.ValooTopBar
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
    onOpenScan: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenHaul: () -> Unit,
    onOpenSettings: () -> Unit,
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
    var editTarget by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddManual by remember { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }
    // Categories already in the catalog — offered as suggestions when re-categorising an item.
    val categories = inventory.mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted()
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
            // Home/catalog bar: no back (it's the start), a few destinations, and an overflow menu.
            ValooTopBar(
                title = "VALOO",
                actions = {
                    IconButton(onClick = onOpenHaul) {
                        Icon(Icons.Default.Collections, contentDescription = stringResource(R.string.cd_haul))
                    }
                    IconButton(onClick = onOpenDiscover) {
                        Icon(Icons.Default.TravelExplore, contentDescription = stringResource(R.string.scan_cd_discover))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.scan_cd_settings))
                    }
                    IconButton(onClick = { exportMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.collection_cd_export))
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.collection_add_manual)) },
                            onClick = { exportMenu = false; showAddManual = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.collection_export_inventory)) },
                            onClick = {
                                exportMenu = false
                                shareCsv(context, "valoo-inventory.csv", viewModel.inventoryCsv())
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.collection_export_history)) },
                            onClick = {
                                exportMenu = false
                                shareCsv(context, "valoo-history.csv", viewModel.historyCsv())
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            // The + opens the scanner (the primary "add" path), CLZ-style.
            FloatingActionButton(onClick = onOpenScan) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.scan_title))
            }
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
                0 -> InventoryList(inventory, onSellClick = { sellTarget = it }, onEditClick = { editTarget = it })
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

    editTarget?.let { item ->
        EditItemDialog(
            item = item,
            categories = categories,
            onConfirm = { title, category, buyPrice, resale ->
                viewModel.updateItem(item.id, title, category, buyPrice, resale)
                editTarget = null
            },
            onDelete = {
                viewModel.deleteItem(item.id)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    if (showAddManual) {
        AddManualDialog(
            onConfirm = { title, buyPrice, resale ->
                viewModel.addManual(title, buyPrice, resale)
                showAddManual = false
            },
            onDismiss = { showAddManual = false },
        )
    }
}

@Composable
private fun AddManualDialog(
    onConfirm: (title: String, buyPrice: Money, estimatedResale: Money) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var buyText by remember { mutableStateOf("") }
    var resaleText by remember { mutableStateOf("") }

    // Need a title and a valid price paid; expected resale is optional (blank → falls back to buy).
    val buyEuros = buyText.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collection_add_manual)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.collection_manual_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = buyText,
                    onValueChange = { buyText = it },
                    label = { Text(stringResource(R.string.collection_manual_buy_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = resaleText,
                    onValueChange = { resaleText = it },
                    label = { Text(stringResource(R.string.collection_manual_resale_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && buyEuros != null,
                onClick = {
                    val buy = Money.ofEuros(buyEuros ?: return@TextButton)
                    val est = resaleText.replace(',', '.').toDoubleOrNull()?.let { Money.ofEuros(it) } ?: buy
                    onConfirm(title.trim(), buy, est)
                },
            ) { Text(stringResource(R.string.result_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun EditItemDialog(
    item: InventoryItem,
    categories: List<String>,
    onConfirm: (title: String, category: String?, buyPrice: Money, estimatedResale: Money) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(item.title) }
    var category by remember { mutableStateOf(item.category.orEmpty()) }
    var buyText by remember { mutableStateOf("%.2f".format(item.buyPrice.euros)) }
    var resaleText by remember { mutableStateOf("%.2f".format(item.estimatedResale.euros)) }
    var catMenu by remember { mutableStateOf(false) }

    val buyEuros = buyText.replace(',', '.').toDoubleOrNull()
    val resaleEuros = resaleText.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collection_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.collection_manual_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Category: free text, with the catalog's existing categories as quick-pick suggestions.
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(stringResource(R.string.collection_category_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (categories.isEmpty()) null else {
                            {
                                IconButton(onClick = { catMenu = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.collection_category_pick))
                                }
                            }
                        },
                    )
                    DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { category = c; catMenu = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = buyText,
                    onValueChange = { buyText = it },
                    label = { Text(stringResource(R.string.collection_manual_buy_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = resaleText,
                    onValueChange = { resaleText = it },
                    label = { Text(stringResource(R.string.collection_manual_resale_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.collection_delete)) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && buyEuros != null && resaleEuros != null,
                onClick = {
                    onConfirm(
                        title.trim(),
                        category.trim().takeIf { it.isNotEmpty() },
                        Money.ofEuros(buyEuros ?: return@TextButton),
                        Money.ofEuros(resaleEuros ?: return@TextButton),
                    )
                },
            ) { Text(stringResource(R.string.collection_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
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
private fun InventoryList(
    items: List<InventoryItem>,
    onSellClick: (InventoryItem) -> Unit,
    onEditClick: (InventoryItem) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(stringResource(R.string.collection_empty_inventory))
        return
    }
    // CLZ-style catalog: bucket the inventory into folders by category, each collapsible with a
    // count badge. Items without a category land in an "Overig" folder.
    val otherLabel = stringResource(R.string.collection_folder_other)
    val groups = items
        .groupBy { it.category?.trim()?.takeIf(String::isNotEmpty) ?: otherLabel }
        .toSortedMap(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { (folder, folderItems) ->
            val isOpen = expanded[folder] ?: true
            item(key = "folder-$folder") {
                FolderHeader(
                    title = folder,
                    count = folderItems.size,
                    expanded = isOpen,
                    onToggle = { expanded[folder] = !isOpen },
                )
            }
            if (isOpen) {
                items(folderItems, key = { it.id }) { item ->
                    InventoryRow(item, onSellClick, onEditClick)
                }
            }
        }
    }
}

@Composable
private fun FolderHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "$count",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun InventoryRow(
    item: InventoryItem,
    onSellClick: (InventoryItem) -> Unit,
    onEditClick: (InventoryItem) -> Unit,
) {
            Card(modifier = Modifier.fillMaxWidth().clickable { onEditClick(item) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Catalog-style row: cover thumbnail (monogram when none) + title + price badge.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            val img = item.imageUrl
                            if (img != null) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    item.title.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(item.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                item.buyPrice.toString(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
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
