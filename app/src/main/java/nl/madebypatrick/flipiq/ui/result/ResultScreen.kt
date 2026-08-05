package nl.madebypatrick.flipiq.ui.result

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.domain.model.BuyTierLevel
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.FlipRecommendation
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ScanAnalysis
import nl.madebypatrick.flipiq.domain.model.SourcePriceGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onScanFront: () -> Unit = {},
    onEditSearch: (String) -> Unit = {},
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isWishlisted by viewModel.isWishlisted.collectAsStateWithLifecycle()
    val hasAlert by viewModel.hasAlert.collectAsStateWithLifecycle()
    val title = (state as? ResultUiState.Success)?.analysis?.product?.title ?: stringResource(R.string.result_analyzing)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    var showAlertDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showBuyDialog by remember { mutableStateOf(false) }
    val hasTitle = state is ResultUiState.Success
    // The scored result (if any), used to drive the docked "I bought this" bar + its dialog.
    val successRec = (state as? ResultUiState.Success)?.analysis?.recommendation
    val canBuy = successRec != null && (successRec.stats.hasData || successRec.bestBuyPrice != null)
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Alert is stored regardless; notifications simply stay silent until granted. */ }

    fun openAlertDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        showAlertDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (hasTitle) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.result_cd_edit_search))
                        }
                    }
                    IconButton(onClick = { openAlertDialog() }) {
                        Icon(
                            if (hasAlert) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = if (hasAlert) stringResource(R.string.result_cd_alert_set) else stringResource(R.string.result_cd_set_alert),
                        )
                    }
                    IconButton(onClick = viewModel::toggleWishlist) {
                        Icon(
                            if (isWishlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isWishlisted) stringResource(R.string.result_cd_remove_wishlist) else stringResource(R.string.result_cd_add_wishlist),
                        )
                    }
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) stringResource(R.string.result_cd_remove_favorites) else stringResource(R.string.result_cd_add_favorites),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { if (canBuy) BottomBuyBar { showBuyDialog = true } },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                is ResultUiState.Loading -> LoadingState()
                is ResultUiState.Error -> Text(stringResource(R.string.result_error_format, s.message), modifier = Modifier.padding(24.dp))
                is ResultUiState.Success -> AnalysisContent(
                    analysis = s.analysis,
                    onConditionChange = viewModel::setCondition,
                    onCompletenessChange = viewModel::setCompleteness,
                    onScanFront = onScanFront,
                )
            }
        }
    }

    if (showAlertDialog) {
        val rec = (state as? ResultUiState.Success)?.analysis?.recommendation
        val suggested = rec?.bestBuyPrice ?: rec?.recommendedBuyPrice ?: Money.ofEuros(10.0)
        PriceInputDialog(
            titleText = stringResource(R.string.result_alert_title),
            label = stringResource(R.string.result_alert_price_label),
            confirmText = stringResource(R.string.result_alert_confirm),
            suggested = suggested,
            onConfirm = { price ->
                showAlertDialog = false
                price?.let { viewModel.setPriceAlert(it) }
            },
            onDismiss = { showAlertDialog = false },
        )
    }

    if (showEditDialog) {
        EditSearchDialog(
            initial = title,
            onConfirm = { edited ->
                showEditDialog = false
                if (edited.isNotBlank() && edited != title) onEditSearch(edited)
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showBuyDialog && successRec != null) {
        PriceInputDialog(
            titleText = stringResource(R.string.result_add_inventory_title),
            label = stringResource(R.string.result_buy_price_label),
            confirmText = stringResource(R.string.result_add),
            suggested = successRec.recommendedBuyPrice,
            onConfirm = { price ->
                showBuyDialog = false
                viewModel.markAsBought(price)
            },
            onDismiss = { showBuyDialog = false },
        )
    }
}

/** Fix an off-title (e.g. a slightly-wrong AI identification) and re-run the search. */
@Composable
private fun EditSearchDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.result_edit_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.result_edit_search_term_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.result_search))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LoadingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.result_loading), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Box-art / listing photo for the scanned item (from a marketplace listing when available). */
@Composable
private fun ProductImage(url: String) {
    AsyncImage(
        model = url,
        contentDescription = stringResource(R.string.result_cd_product_image),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun AnalysisContent(
    analysis: ScanAnalysis,
    onConditionChange: (Condition) -> Unit,
    onCompletenessChange: (Completeness) -> Unit,
    onScanFront: () -> Unit,
) {
    val rec = analysis.recommendation
    // No sold data and nothing currently for sale → we can't score it; guide to the links instead.
    val hasData = rec.stats.hasData || rec.bestBuyPrice != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        analysis.product.imageUrl?.let { ProductImage(it) }
        when {
            analysis.allSourcesDisabled -> SourcesOffCard()
            hasData -> {
                // The market view — eBay sold history + Marktplaats, aggregated by the engine.
                PriceSummaryCard(rec)
                if (analysis.pricesBySource.isNotEmpty()) PricesBySourceCard(analysis.pricesBySource)
                VerdictCard(rec)
                ConditionSelectors(analysis.condition, analysis.completeness, onConditionChange, onCompletenessChange)
                EstimateCard(rec)
                BuyLadderCard(rec)
                if (rec.notes.isNotEmpty()) NotesCard(rec)
            }
            else -> NoDataCard(analysis.product.title, onScanFront = onScanFront)
        }
        if (!analysis.allSourcesDisabled) MarketplaceShortcutsCard(analysis)
        Spacer(Modifier.height(8.dp))
    }
}

/** CLZ-style docked action bar: the primary "I bought this" button pinned to the bottom. */
@Composable
private fun BottomBuyBar(onClick: () -> Unit) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
        ) { Text(stringResource(R.string.result_i_bought), style = MaterialTheme.typography.titleMedium) }
    }
}

/** Reusable "enter a euro price" dialog, pre-filled with a suggestion. */
@Composable
private fun PriceInputDialog(
    titleText: String,
    label: String,
    confirmText: String,
    suggested: Money,
    onConfirm: (Money?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("%.2f".format(suggested.euros)) }
    val parsed = text.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsed?.let { Money.ofEuros(it) }) },
                enabled = parsed != null,
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun NoDataCard(title: String, onScanFront: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🕵️", fontSize = 40.sp)
            Text(
                stringResource(R.string.result_nodata_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (title == "Unknown item") {
                    stringResource(R.string.result_nodata_unknown)
                } else {
                    stringResource(R.string.result_nodata_no_prices, title)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onScanFront, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.result_scan_front_instead))
            }
            Text(
                stringResource(R.string.result_nodata_tip),
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun VerdictCard(rec: FlipRecommendation) {
    val tierColor = rec.dealScore.tier.color
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tierColor.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(rec.dealScore.tier.emoji, fontSize = 44.sp)
            Text(
                rec.dealScore.tier.headline,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = tierColor,
            )
            Text(
                stringResource(R.string.result_deal_score, rec.dealScore.value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AssistChip(onClick = {}, label = { Text("${rec.sellSpeed.emoji} ${rec.sellSpeed.label}") })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.result_confidence, rec.confidence)) })
                AssistChip(onClick = {}, label = { Text(rec.trend.label) })
            }
            if (!rec.viable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.result_not_viable),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConditionSelectors(
    condition: Condition,
    completeness: Completeness,
    onConditionChange: (Condition) -> Unit,
    onCompletenessChange: (Completeness) -> Unit,
) {
    SectionCard(stringResource(R.string.result_condition_section)) {
        Text(stringResource(R.string.result_condition_label), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Condition.entries.forEach { c ->
                FilterChip(
                    selected = c == condition,
                    onClick = { onConditionChange(c) },
                    label = { Text(c.label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.result_completeness_label), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Completeness.entries.forEach { c ->
                FilterChip(
                    selected = c == completeness,
                    onClick = { onCompletenessChange(c) },
                    label = { Text(c.label) },
                )
            }
        }
    }
}

/** Where each price came from — one row per marketplace, with its own price range, sold vs asking. */
@Composable
private fun PricesBySourceCard(groups: List<SourcePriceGroup>) {
    SectionCard(stringResource(R.string.result_by_source_section)) {
        groups.forEach { g ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(g.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    val kind = stringResource(
                        if (g.sold) R.string.result_by_source_sold else R.string.result_by_source_asking,
                    )
                    Text(
                        stringResource(R.string.result_by_source_meta, kind, g.count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(g.median.toString(), fontWeight = FontWeight.Bold)
                    if (g.low != g.high) {
                        Text(
                            stringResource(R.string.result_by_source_range, g.low.toString(), g.high.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceSummaryCard(rec: FlipRecommendation) {
    SectionCard(stringResource(R.string.result_sold_prices, rec.stats.soldCount)) {
        PriceGrid(
            listOf(
                stringResource(R.string.result_stat_avg) to rec.stats.average.toString(),
                stringResource(R.string.result_stat_median) to rec.stats.median.toString(),
                stringResource(R.string.result_stat_low) to rec.stats.lowest.toString(),
                stringResource(R.string.result_stat_high) to rec.stats.highest.toString(),
            ),
        )
    }
}

@Composable
private fun EstimateCard(rec: FlipRecommendation) {
    SectionCard(stringResource(R.string.result_estimate_section)) {
        StatRow(stringResource(R.string.result_est_resale), rec.estimatedResale.toString())
        StatRow(stringResource(R.string.result_net_after), rec.netResale.toString())
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        StatRow(stringResource(R.string.result_recommended_max), rec.recommendedBuyPrice.toString(), emphasise = true)
        StatRow(stringResource(R.string.result_expected_profit), rec.expectedProfit.toString())
        StatRow(stringResource(R.string.result_roi), stringResource(R.string.result_roi_value, rec.roiPercent))
    }
}

@Composable
private fun SourcesOffCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🔌", fontSize = 40.sp)
            Text(
                stringResource(R.string.result_sources_off_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.result_sources_off_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BuyLadderCard(rec: FlipRecommendation) {
    SectionCard(stringResource(R.string.result_buy_ladder)) {
        rec.buyTiers.forEach { tier ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(tier.level.label, color = tier.level.color, fontWeight = FontWeight.Medium)
                val prefix = if (tier.level == BuyTierLevel.SKIP) "> " else "≤ "
                Text("$prefix${tier.maxPrice}")
            }
        }
    }
}

@Composable
private fun NotesCard(rec: FlipRecommendation) {
    SectionCard(stringResource(R.string.result_notes_section)) {
        rec.notes.forEach { note ->
            Text("• $note", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MarketplaceShortcutsCard(analysis: ScanAnalysis) {
    val context = LocalContext.current
    SectionCard(stringResource(R.string.result_marketplace_section)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            analysis.sources.filter { it.shortcutUrl != null }.forEach { source ->
                AssistChip(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.shortcutUrl)))
                        }
                    },
                    label = {
                        val n = source.listingCount
                        Text(if (n > 0) "${source.displayName} ($n)" else source.displayName)
                    },
                    trailingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}

// --- Small building blocks ------------------------------------------------------------------

/** Catalog-style card: a coloured brand header strip with the section title, then the content. */
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                content()
            }
        }
    }
}

/** A bordered price grid (à la a catalog price table): equal cells, each a value over its label. */
@Composable
private fun PriceGrid(cells: List<Pair<String, String>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
    ) {
        cells.forEachIndexed { i, (label, value) ->
            if (i > 0) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, emphasise: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasise) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )
    }
}
