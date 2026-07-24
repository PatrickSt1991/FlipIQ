package nl.madebypatrick.flipiq.ui.result

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.FlipRecommendation
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ScanAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isWishlisted by viewModel.isWishlisted.collectAsStateWithLifecycle()
    val title = (state as? ResultUiState.Success)?.analysis?.product?.title ?: "Analyzing…"

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleWishlist) {
                        Icon(
                            if (isWishlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                        )
                    }
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                is ResultUiState.Loading -> LoadingState()
                is ResultUiState.Error -> Text("⚠ ${s.message}", modifier = Modifier.padding(24.dp))
                is ResultUiState.Success -> AnalysisContent(
                    analysis = s.analysis,
                    onConditionChange = viewModel::setCondition,
                    onCompletenessChange = viewModel::setCompleteness,
                    onMarkBought = viewModel::markAsBought,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Comparing marketplaces…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AnalysisContent(
    analysis: ScanAnalysis,
    onConditionChange: (Condition) -> Unit,
    onCompletenessChange: (Completeness) -> Unit,
    onMarkBought: (Money?) -> Unit,
) {
    val rec = analysis.recommendation
    var showBuyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VerdictCard(rec)
        Button(
            onClick = { showBuyDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("I bought this") }
        ConditionSelectors(analysis.condition, analysis.completeness, onConditionChange, onCompletenessChange)
        PriceSummaryCard(rec)
        EstimateCard(rec)
        BuyLadderCard(rec)
        if (rec.notes.isNotEmpty()) NotesCard(rec)
        MarketplaceShortcutsCard(analysis)
        Spacer(Modifier.height(8.dp))
    }

    if (showBuyDialog) {
        BuyPriceDialog(
            suggested = rec.recommendedBuyPrice,
            onConfirm = { price ->
                showBuyDialog = false
                onMarkBought(price)
            },
            onDismiss = { showBuyDialog = false },
        )
    }
}

@Composable
private fun BuyPriceDialog(
    suggested: Money,
    onConfirm: (Money?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("%.2f".format(suggested.euros)) }
    val parsed = text.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to inventory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What did you pay for it?")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Buy price (€)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsed?.let { Money.ofEuros(it) }) },
                enabled = parsed != null,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "${rec.dealScore.value}",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = tierColor,
            )
            Text("Deal Score · out of 100", style = MaterialTheme.typography.labelMedium)
            Text(
                rec.dealScore.tier.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tierColor,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${rec.sellSpeed.emoji} ${rec.sellSpeed.label}") })
                AssistChip(onClick = {}, label = { Text("Confidence ${rec.confidence}%") })
                AssistChip(onClick = {}, label = { Text(rec.trend.label) })
            }
            if (!rec.viable) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Doesn't meet your Profit Mode targets",
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
    SectionCard("Item condition") {
        Text("Condition", style = MaterialTheme.typography.labelLarge)
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
        Text("Completeness", style = MaterialTheme.typography.labelLarge)
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

@Composable
private fun PriceSummaryCard(rec: FlipRecommendation) {
    SectionCard("Sold prices (${rec.stats.soldCount} sales)") {
        StatRow("Average sold", rec.stats.average.toString())
        StatRow("Median sold", rec.stats.median.toString())
        StatRow("Lowest", rec.stats.lowest.toString())
        StatRow("Highest", rec.stats.highest.toString())
    }
}

@Composable
private fun EstimateCard(rec: FlipRecommendation) {
    SectionCard("FlipIQ estimate") {
        StatRow("Estimated resale", rec.estimatedResale.toString())
        StatRow("Net after fees & shipping", rec.netResale.toString())
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        StatRow("Recommended max buy", rec.recommendedBuyPrice.toString(), emphasise = true)
        StatRow("Expected profit", rec.expectedProfit.toString())
        StatRow("ROI", "${rec.roiPercent}%")
    }
}

@Composable
private fun BuyLadderCard(rec: FlipRecommendation) {
    SectionCard("Buy ladder") {
        rec.buyTiers.forEach { tier ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(tier.level.label, color = tier.level.color, fontWeight = FontWeight.Medium)
                val prefix = if (tier.level.label == "Skip above") "> " else "≤ "
                Text("$prefix${tier.maxPrice}")
            }
        }
    }
}

@Composable
private fun NotesCard(rec: FlipRecommendation) {
    SectionCard("Why") {
        rec.notes.forEach { note ->
            Text("• $note", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MarketplaceShortcutsCard(analysis: ScanAnalysis) {
    val context = LocalContext.current
    SectionCard("Open on marketplace") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            analysis.sources.filter { it.shortcutUrl != null }.forEach { source ->
                AssistChip(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.shortcutUrl)))
                        }
                    },
                    label = { Text("${source.displayName} (${source.listingCount})") },
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

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
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
