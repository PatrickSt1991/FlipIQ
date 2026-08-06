package nl.madebypatrick.flipiq.ui.collection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.InventoryStatus
import nl.madebypatrick.flipiq.domain.model.InventorySummary
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.ui.components.ValooTopBar
import nl.madebypatrick.flipiq.ui.util.shareCsv

/** How the catalog lays out items. */
private enum class CatalogView { GRID, LIST }

/** How the catalog is ordered. */
private enum class CatalogSort { NEWEST, TITLE, VALUE, PROFIT }

/** How the catalog is bucketed into folders (NONE = a flat list, no folders). */
private enum class CatalogGroup { CATEGORY, STATUS, NONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onOpenScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onDrilledChange: (Boolean) -> Unit = {},
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    var sellTarget by remember { mutableStateOf<InventoryItem?>(null) }
    var editTarget by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddManual by remember { mutableStateOf(false) }
    var showProfit by remember { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }

    // Catalog browse state (CLZ-style): a group folder we've drilled into, an item we've opened, and
    // the current view/sort/group choices.
    var openFolder by remember { mutableStateOf<String?>(null) }
    var detailItemId by remember { mutableStateOf<Long?>(null) }
    var view by remember { mutableStateOf(CatalogView.GRID) }
    var sort by remember { mutableStateOf(CatalogSort.NEWEST) }
    var group by remember { mutableStateOf(CatalogGroup.CATEGORY) }

    // The opened item is re-derived from the live list so edits/deletes reflect immediately.
    val detailItem = detailItemId?.let { id -> inventory.firstOrNull { it.id == id } }
    val inDetail = detailItem != null
    val inFolder = openFolder != null && !inDetail

    // Categories already in the catalog — offered as suggestions when re-categorising an item.
    val categories = inventory.mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted()

    // Folder key for an item under the current grouping (also used to filter a drilled-in folder).
    val otherLabel = stringResource(R.string.collection_folder_other)
    val statusInStock = stringResource(R.string.collection_status_in_stock)
    val statusListed = stringResource(R.string.collection_status_listed)
    val statusSold = stringResource(R.string.collection_status_sold)
    val folderKey: (InventoryItem) -> String = { item ->
        when (group) {
            CatalogGroup.CATEGORY -> item.category?.trim()?.takeIf(String::isNotEmpty) ?: otherLabel
            CatalogGroup.STATUS -> when (item.status) {
                InventoryStatus.IN_STOCK -> statusInStock
                InventoryStatus.LISTED -> statusListed
                InventoryStatus.SOLD -> statusSold
            }
            CatalogGroup.NONE -> ""
        }
    }

    val context = LocalContext.current

    val goBack = { if (inDetail) detailItemId = null else openFolder = null }
    BackHandler(enabled = inDetail || inFolder) { goBack() }
    // Tell the host pager whether we're drilled in, so it can suspend its own left/right swipe while
    // the folder/detail view (and the detail's own pager) owns horizontal gestures.
    LaunchedEffect(inDetail, inFolder) { onDrilledChange(inDetail || inFolder) }

    Scaffold(
        topBar = {
            ValooTopBar(
                title = when {
                    inDetail -> detailItem!!.title
                    inFolder -> openFolder!!
                    else -> "VALOO"
                },
                navigationIcon = {
                    if (inDetail || inFolder) {
                        IconButton(onClick = goBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
                actions = {
                    // The settings/overflow only belong on the catalog home, not the drill-ins.
                    // (Ontdekken/Partij now live in the bottom navigation.)
                    if (!inDetail && !inFolder) {
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
                    }
                },
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Profit tracker tucked under a € button so the catalog stays front-and-centre.
                if (!inDetail && !inFolder) {
                    SmallFloatingActionButton(
                        onClick = { showProfit = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(Icons.Default.EuroSymbol, contentDescription = stringResource(R.string.collection_profit_tracker))
                    }
                }
                // The + opens the scanner (the primary "add" path). Hidden on the detail page.
                if (!inDetail) {
                    FloatingActionButton(onClick = onOpenScan) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.scan_title))
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                inDetail -> {
                    // Swipe left/right through the items of the list this one was opened from
                    // (the current folder, or the flat list), respecting the active sort.
                    val contextItems = if (openFolder != null) {
                        sortItems(inventory.filter { folderKey(it) == openFolder }, sort)
                    } else {
                        sortItems(inventory, sort)
                    }
                    ItemDetailPager(
                        items = contextItems,
                        currentId = detailItem!!.id,
                        onPageChange = { detailItemId = it.id },
                        onEdit = { editTarget = it },
                        onSell = { sellTarget = it },
                        onDelete = { viewModel.deleteItem(it.id); detailItemId = null },
                    )
                }

                inFolder -> {
                    val folderItems = sortItems(inventory.filter { folderKey(it) == openFolder }, sort)
                    CatalogControls(
                        group = group,
                        onGroupChange = { group = it },
                        showGroup = false,
                        view = view,
                        onViewChange = { view = it },
                        sort = sort,
                        onSortChange = { sort = it },
                    )
                    ItemsView(folderItems, view, onOpenItem = { detailItemId = it.id })
                }

                else -> CatalogTab(
                    items = inventory,
                    group = group,
                    onGroupChange = { group = it },
                    view = view,
                    onViewChange = { view = it },
                    sort = sort,
                    onSortChange = { sort = it },
                    folderKey = folderKey,
                    onOpenFolder = { openFolder = it },
                    onOpenItem = { detailItemId = it.id },
                )
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
                detailItemId = null
            },
            onDismiss = { editTarget = null },
        )
    }

    if (showProfit) {
        ProfitDialog(summary = summary, onDismiss = { showProfit = false })
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

/** Order a list of items by the chosen [CatalogSort]. */
private fun sortItems(items: List<InventoryItem>, sort: CatalogSort): List<InventoryItem> = when (sort) {
    CatalogSort.NEWEST -> items.sortedByDescending { it.boughtAt }
    CatalogSort.TITLE -> items.sortedBy { it.title.lowercase() }
    CatalogSort.VALUE -> items.sortedByDescending { it.estimatedResale.cents }
    CatalogSort.PROFIT -> items.sortedByDescending { it.projectedProfit.cents }
}

/** The tab-0 catalog: controls, then either folders (grouped) or a flat item view. */
@Composable
private fun CatalogTab(
    items: List<InventoryItem>,
    group: CatalogGroup,
    onGroupChange: (CatalogGroup) -> Unit,
    view: CatalogView,
    onViewChange: (CatalogView) -> Unit,
    sort: CatalogSort,
    onSortChange: (CatalogSort) -> Unit,
    folderKey: (InventoryItem) -> String,
    onOpenFolder: (String) -> Unit,
    onOpenItem: (InventoryItem) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(stringResource(R.string.collection_empty_inventory))
        return
    }
    CatalogControls(
        group = group,
        onGroupChange = onGroupChange,
        showGroup = true,
        view = view,
        onViewChange = onViewChange,
        sort = sort,
        onSortChange = onSortChange,
    )
    if (group == CatalogGroup.NONE) {
        ItemsView(sortItems(items, sort), view, onOpenItem)
    } else {
        // Folder rows: grouped by the current key, sorted by name, each with a count badge.
        val folders = items.groupBy(folderKey).toSortedMap(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            folders.forEach { (name, folderItems) ->
                item(key = "folder-$name") {
                    FolderRow(name = name, count = folderItems.size, onClick = { onOpenFolder(name) })
                }
            }
        }
    }
}

/** Group chip (📁 …) + view toggle + sort menu — the CLZ catalog control strip. */
@Composable
private fun CatalogControls(
    group: CatalogGroup,
    onGroupChange: (CatalogGroup) -> Unit,
    showGroup: Boolean,
    view: CatalogView,
    onViewChange: (CatalogView) -> Unit,
    sort: CatalogSort,
    onSortChange: (CatalogSort) -> Unit,
) {
    var groupMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showGroup) {
            Box {
                AssistChip(
                    onClick = { groupMenu = true },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    label = { Text(groupLabel(group)) },
                )
                DropdownMenu(expanded = groupMenu, onDismissRequest = { groupMenu = false }) {
                    CatalogGroup.entries.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(groupLabel(g)) },
                            onClick = { onGroupChange(g); groupMenu = false },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // View toggle only matters where items are shown (flat list, or inside a folder).
        if (!showGroup || group == CatalogGroup.NONE) {
            IconButton(onClick = { onViewChange(if (view == CatalogView.GRID) CatalogView.LIST else CatalogView.GRID) }) {
                Icon(
                    if (view == CatalogView.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = stringResource(
                        if (view == CatalogView.GRID) R.string.catalog_view_list else R.string.catalog_view_grid,
                    ),
                )
            }
        }
        Box {
            IconButton(onClick = { sortMenu = true }) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.catalog_sort_cd))
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                CatalogSort.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(sortLabel(s)) },
                        trailingIcon = { if (s == sort) Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        onClick = { onSortChange(s); sortMenu = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun groupLabel(group: CatalogGroup): String = stringResource(
    when (group) {
        CatalogGroup.CATEGORY -> R.string.catalog_group_category
        CatalogGroup.STATUS -> R.string.catalog_group_status
        CatalogGroup.NONE -> R.string.catalog_group_none
    },
)

@Composable
private fun sortLabel(sort: CatalogSort): String = stringResource(
    when (sort) {
        CatalogSort.NEWEST -> R.string.catalog_sort_newest
        CatalogSort.TITLE -> R.string.catalog_sort_title
        CatalogSort.VALUE -> R.string.catalog_sort_value
        CatalogSort.PROFIT -> R.string.catalog_sort_profit
    },
)

@Composable
private fun FolderRow(name: String, count: Int, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            CountBadge(count)
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            "$count",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Items as either a cover grid or a list of rows. */
@Composable
private fun ItemsView(items: List<InventoryItem>, view: CatalogView, onOpenItem: (InventoryItem) -> Unit) {
    if (items.isEmpty()) {
        EmptyState(stringResource(R.string.collection_empty_inventory))
        return
    }
    if (view == CatalogView.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }) { item -> ItemGridCell(item, onOpenItem) }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id }) { item -> ItemListRow(item, onOpenItem) }
        }
    }
}

@Composable
private fun ItemGridCell(item: InventoryItem, onOpenItem: (InventoryItem) -> Unit) {
    Column(modifier = Modifier.clickable { onOpenItem(item) }) {
        Cover(item, modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.height(4.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            item.estimatedResale.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ItemListRow(item: InventoryItem, onOpenItem: (InventoryItem) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenItem(item) }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(item, modifier = Modifier.size(48.dp, 64.dp).clip(RoundedCornerShape(6.dp)))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.category?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    item.estimatedResale.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Cover art with a coloured monogram fallback. */
@Composable
private fun Cover(item: InventoryItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
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
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** A horizontal pager over the current list so the detail page can be swiped left/right. */
@Composable
private fun ItemDetailPager(
    items: List<InventoryItem>,
    currentId: Long,
    onPageChange: (InventoryItem) -> Unit,
    onEdit: (InventoryItem) -> Unit,
    onSell: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit,
) {
    if (items.isEmpty()) return
    val itemsState = rememberUpdatedState(items)
    val startIndex = items.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex) { itemsState.value.size }

    // Report the settled page up so the app-bar title and back target track the visible item.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            itemsState.value.getOrNull(page)?.let(onPageChange)
        }
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val item = itemsState.value.getOrElse(page) { itemsState.value.last() }
        ItemDetailPane(
            item = item,
            onEdit = { onEdit(item) },
            onSell = { onSell(item) },
            onDelete = { onDelete(item) },
        )
    }
}

/** Full item detail page: big cover, title, status/category, price grid and actions. */
@Composable
private fun ItemDetailPane(
    item: InventoryItem,
    onEdit: () -> Unit,
    onSell: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Cover(item, modifier = Modifier.size(120.dp, 160.dp).clip(RoundedCornerShape(10.dp)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                item.category?.takeIf { it.isNotBlank() }?.let {
                    AssistChip(onClick = onEdit, label = { Text(it) })
                }
                StatusChip(item.status)
            }
        }

        // Price grid.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow(stringResource(R.string.collection_detail_buy), item.buyPrice.toString())
                if (item.status == InventoryStatus.SOLD) {
                    SummaryRow(stringResource(R.string.collection_sold_for), item.soldPrice?.toString() ?: "—")
                    SummaryRow(stringResource(R.string.collection_profit), item.realizedProfit?.toString() ?: "—", emphasise = true)
                } else {
                    SummaryRow(stringResource(R.string.collection_est_resale), item.estimatedResale.toString())
                    SummaryRow(stringResource(R.string.collection_projected_profit), item.projectedProfit.toString(), emphasise = true)
                }
            }
        }

        // Actions.
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.collection_edit))
        }
        if (item.status != InventoryStatus.SOLD) {
            OutlinedButton(onClick = onSell, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Sell, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.collection_mark_sold))
            }
        }
        TextButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.collection_delete))
        }
    }
}

@Composable
private fun StatusChip(status: InventoryStatus) {
    val label = stringResource(
        when (status) {
            InventoryStatus.IN_STOCK -> R.string.collection_status_in_stock
            InventoryStatus.LISTED -> R.string.collection_status_listed
            InventoryStatus.SOLD -> R.string.collection_status_sold
        },
    )
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
private fun ProfitDialog(summary: InventorySummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collection_profit_tracker)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryRow(stringResource(R.string.collection_in_stock), stringResource(R.string.collection_in_stock_value, summary.itemsInStock, summary.capitalInStock.toString()))
                SummaryRow(stringResource(R.string.collection_sold), stringResource(R.string.collection_sold_value, summary.itemsSold))
                SummaryRow(stringResource(R.string.collection_realized_profit), summary.realizedProfit.toString(), emphasise = true)
                SummaryRow(stringResource(R.string.collection_projected_profit), summary.projectedProfit.toString())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) }
        },
    )
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
