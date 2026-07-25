package nl.madebypatrick.flipiq.ui.scan

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.madebypatrick.flipiq.R
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
    onBarcodeScanned: (String) -> Unit,
    onSearchTitle: (String) -> Unit,
    onOpenCollection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    // Front (OCR the box) is the default: without a complete EAN database, a barcode often can't be
    // resolved to a title, and the real sources (eBay, Marktplaats) search by title anyway.
    var mode by remember { mutableStateOf(ScanMode.FRONT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.flipiq_mark),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("FlipIQ")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistics")
                    }
                    IconButton(onClick = onOpenCollection) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Collection")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Profit Mode settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ModeToggle(mode = mode, onModeChange = { mode = it })
            Spacer(Modifier.height(16.dp))

            when (mode) {
                ScanMode.FRONT -> FrontScanPane(
                    onSearch = onSearchTitle,
                    modifier = Modifier.fillMaxWidth(),
                )

                ScanMode.BARCODE -> {
                    if (cameraPermission.status.isGranted) {
                        CameraScanner(
                            onBarcode = onBarcodeScanned,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Point at a barcode and we'll do the math ✨",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeToggle(mode: ScanMode, onModeChange: (ScanMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = mode == ScanMode.FRONT,
            onClick = { onModeChange(ScanMode.FRONT) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("📸 Front") }
        SegmentedButton(
            selected = mode == ScanMode.BARCODE,
            onClick = { onModeChange(ScanMode.BARCODE) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("⁞⁞ Barcode") }
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
                "Turn on the camera and start scanning in a tap.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRequest) { Text("Enable camera") }
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
            Text("Or enter a barcode", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("Barcode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { if (text.isNotBlank()) onSubmit(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Analyze") }

            Text("Try a sample:", style = MaterialTheme.typography.labelLarge)
            SampleBarcodes.forEach { (label, code) ->
                OutlinedButton(
                    onClick = { onSubmit(code) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(label) }
            }
        }
    }
}

private val SampleBarcodes = listOf(
    "LEGO Jurassic World (PS4)" to "5051888223451",
    "The Last of Us Part II (PS4)" to "0711719417972",
    "LEGO Technic Bugatti Chiron" to "5702016367447",
)

@Composable
private fun CameraScanner(
    onBarcode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, BarcodeAnalyzer(onBarcode)) }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
