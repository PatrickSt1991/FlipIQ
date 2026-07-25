package nl.madebypatrick.flipiq.ui.scan

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * OCR fallback with a **snapshot** flow (fixes the "never stops scanning" problem):
 * aim at the product's front → tap **Capture** → the recognised text freezes into an editable field
 * → trim it → **Search**. The camera reads only while aiming; nothing churns.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TextScanScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Latest recognised text from the live camera — NOT shown while aiming (so it doesn't flicker);
    // only read when the user taps Capture.
    var latest by remember { mutableStateOf("") }
    // Null while aiming; a snapshot string once captured (switches to the review/edit state).
    var captured by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan the front") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !cameraPermission.status.isGranted -> ManualFallback(
                    onEnableCamera = { cameraPermission.launchPermissionRequest() },
                    onSearch = onSearch,
                )

                captured == null -> {
                    // Aiming: live camera, no text shown, single Capture action.
                    TextCamera(
                        onText = { latest = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Text(
                        "Aim at the product name, then capture.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { captured = firstMeaningfulLine(latest) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Text("  Capture")
                    }
                }

                else -> CapturedReview(
                    initial = captured.orEmpty(),
                    onRescan = { captured = null },
                    onSearch = onSearch,
                )
            }
        }
    }
}

/** Editable review of the captured text, with Search / Rescan. */
@Composable
private fun CapturedReview(
    initial: String,
    onRescan: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var query by remember { mutableStateOf(initial) }
    Text("Captured — tidy it up if needed", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search text") },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { if (query.isNotBlank()) onSearch(query.trim()) },
        enabled = query.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Search this") }
    OutlinedButton(onClick = onRescan, modifier = Modifier.fillMaxWidth()) { Text("Rescan") }
}

@Composable
private fun ManualFallback(onEnableCamera: () -> Unit, onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Button(onClick = onEnableCamera, modifier = Modifier.fillMaxWidth()) { Text("Enable camera") }
    Text(
        "Or type the product name below.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search text") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { if (query.isNotBlank()) onSearch(query.trim()) },
        enabled = query.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Search this") }
}

/** Product names are usually the largest text; the longest recognised line is a good first guess. */
private fun firstMeaningfulLine(text: String): String =
    text.lines().map { it.trim() }.filter { it.isNotBlank() }.maxByOrNull { it.length }?.take(80)
        ?: text.trim()

@Composable
private fun TextCamera(
    onText: (String) -> Unit,
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
                        .also { it.setAnalyzer(executor, TextRecognitionAnalyzer(onText)) }

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

    // Stop the camera/analysis when this leaves the composition (e.g. after Capture).
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }
}
