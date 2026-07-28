package nl.madebypatrick.flipiq.ui.share

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.ui.scan.ScanViewModel

/**
 * Landing screen for a shared image. Identifies on arrival — the user came from another app expecting
 * an answer, not a scan screen they have to re-photograph from. On failure it shows the reason and a
 * typed fallback so it's never a dead end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedImageScreen(
    uri: Uri?,
    onBack: () -> Unit,
    onTitle: (String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    // Plain remember (not saveable): after a config change mid-upload this resets to false, so once
    // the surviving in-flight call finishes the effect re-runs with a live callback instead of
    // spinning forever. Keyed on identifying so it waits for that call rather than starting a second.
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(uri, viewModel.identifying) {
        if (viewModel.identifying || started) return@LaunchedEffect
        started = true
        if (uri == null) {
            // The read grant didn't survive process death — treat as unreadable, don't just hang.
            message = context.getString(R.string.image_unreadable)
            return@LaunchedEffect
        }
        viewModel.identifyImage(uri) { title, unreadable ->
            if (title != null) {
                onTitle(title)
            } else {
                message = context.getString(
                    if (unreadable) R.string.image_unreadable else R.string.ai_not_identified,
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shared_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (viewModel.identifying) {
                CircularProgressIndicator()
                Text(
                    stringResource(R.string.shared_identifying),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                message?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                TypedFallback(onSearch = onTitle)
            }
        }
    }
}

@Composable
private fun TypedFallback(onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Text(
        stringResource(R.string.ai_type_name),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text(stringResource(R.string.ai_game_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = { if (query.isNotBlank()) onSearch(query.trim()) },
        enabled = query.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.search_this)) }
}
