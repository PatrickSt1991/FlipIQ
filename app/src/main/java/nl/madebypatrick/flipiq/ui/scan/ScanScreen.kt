package nl.madebypatrick.flipiq.ui.scan

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.ui.components.ValooTopBar
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * The scan screen. When camera permission is granted it shows a live CameraX preview wired to the
 * ML Kit [BarcodeAnalyzer]; otherwise (or on an emulator) it offers manual barcode entry and a few
 * sample barcodes so the whole flow is usable without a camera.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onSearchTitle: (String) -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    // Barcode is the default now that the engine resolves EANs well (ean13/buycott + on-device
    // eandata). Front (photograph the cover → AI) is the fallback for items without a readable code.
    var mode by remember { mutableStateOf(ScanMode.BARCODE) }

    Scaffold(
        topBar = {
            ValooTopBar(
                title = stringResource(R.string.scan_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ModeToggle(mode = mode, onModeChange = { mode = it })
            Spacer(Modifier.height(16.dp))

            when (mode) {
                ScanMode.FRONT -> AiScanPane(
                    onSearch = onSearchTitle,
                    modifier = Modifier.fillMaxWidth(),
                )

                ScanMode.BARCODE -> {
                    if (cameraPermission.status.isGranted) {
                        CameraScanner(
                            onBarcode = onBarcodeScanned,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.scan_barcode_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        CameraPermissionPrompt(onRequest = { cameraPermission.launchPermissionRequest() })
                    }
                    Spacer(Modifier.height(24.dp))
                    ManualEntry(onSubmit = onBarcodeScanned)
                }
            }
        }
    }
}

private enum class ScanMode { FRONT, BARCODE }

/** CLZ-style "Add by:" pill bar — a brand-coloured strip with white selected pills. */
@Composable
private fun ModeToggle(mode: ScanMode, onModeChange: (ScanMode) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.scan_add_by),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(12.dp))
            ModePill(stringResource(R.string.scan_mode_front), mode == ScanMode.FRONT) { onModeChange(ScanMode.FRONT) }
            Spacer(Modifier.width(8.dp))
            ModePill(stringResource(R.string.scan_mode_barcode), mode == ScanMode.BARCODE) { onModeChange(ScanMode.BARCODE) }
        }
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraPermissionPrompt(onRequest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.height(48.dp),
            )
            Text(
                stringResource(R.string.scan_camera_prompt),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRequest) { Text(stringResource(R.string.enable_camera)) }
        }
    }
}

@Composable
private fun ManualEntry(onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.scan_manual_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.scan_barcode_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { if (text.isNotBlank()) onSubmit(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.scan_analyze)) }
        }
    }
}

@Composable
private fun CameraScanner(
    onBarcode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    CameraPreview(
        modifier = modifier,
        extraUseCases = {
            listOf(
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), BarcodeAnalyzer(onBarcode)) },
            )
        },
    )
}
