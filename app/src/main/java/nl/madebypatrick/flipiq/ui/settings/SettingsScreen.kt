package nl.madebypatrick.flipiq.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.ThemeMode

/**
 * Profit Mode configuration screen. Edits a local working copy of [ProfitSettings] and persists it
 * (via DataStore) when the user taps Save; the engine picks the new values up on the next scan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val saved by viewModel.settings.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    // Re-seed the working copy whenever the persisted value changes (initial load / after save).
    var edited by remember(saved) { mutableStateOf(saved) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profit Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection("Targets") {
                MoneyField("Minimum profit", edited.minProfit) { edited = edited.copy(minProfit = it) }
                PercentField("Minimum ROI", edited.minRoi) { edited = edited.copy(minRoi = it) }
                MoneyField("Ignore items below", edited.ignoreBelow) { edited = edited.copy(ignoreBelow = it) }
                IntField("Minimum recent sales", edited.minSales) { edited = edited.copy(minSales = it) }
            }

            SettingsSection("Costs") {
                ToggleRow("Include marketplace fees", edited.includeFees) { edited = edited.copy(includeFees = it) }
                PercentField("Marketplace fee", edited.marketplaceFee) { edited = edited.copy(marketplaceFee = it) }
                ToggleRow("Include shipping", edited.includeShipping) { edited = edited.copy(includeShipping = it) }
                MoneyField("Shipping cost", edited.shippingCost) { edited = edited.copy(shippingCost = it) }
            }

            SettingsSection("Filters") {
                ToggleRow("Ignore incomplete items", edited.ignoreIncomplete) { edited = edited.copy(ignoreIncomplete = it) }
                ToggleRow("Ignore damaged items", edited.ignoreDamaged) { edited = edited.copy(ignoreDamaged = it) }
                ToggleRow("Prefer fast sellers", edited.preferFastSellers) { edited = edited.copy(preferFastSellers = it) }
            }

            // Appearance is applied immediately (no Save needed).
            SettingsSection("Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                ThemeModeSelector(theme.mode, viewModel::setThemeMode)
                ToggleRow("Material You colours", theme.dynamicColor, viewModel::setDynamicColor)
            }

            Button(
                onClick = {
                    viewModel.save(edited)
                    scope.launch { snackbarHostState.showSnackbar("Saved") }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = edited != saved,
            ) { Text("Save") }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun MoneyField(label: String, value: Money, onChange: (Money) -> Unit) {
    var text by remember(value) { mutableStateOf("%.2f".format(value.euros)) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.replace(',', '.').toDoubleOrNull()?.let { euros -> onChange(Money.ofEuros(euros)) }
        },
        label = { Text("$label (€)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PercentField(label: String, fraction: Double, onChange: (Double) -> Unit) {
    var text by remember(fraction) { mutableStateOf("%.0f".format(fraction * 100)) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.replace(',', '.').toDoubleOrNull()?.let { pct -> onChange(pct / 100.0) }
        },
        label = { Text("$label (%)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun IntField(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit)
            text.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val labels = mapOf(
        ThemeMode.SYSTEM to "System",
        ThemeMode.LIGHT to "Light",
        ThemeMode.DARK to "Dark",
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(labels.getValue(mode)) },
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
