package nl.madebypatrick.flipiq.ui.scan

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
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
import java.util.concurrent.Executors

/**
 * OCR fallback screen — wraps [FrontScanPane] in a Scaffold with a back button (used from the
 * result screen's no-data card).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScanScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
) {
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
        FrontScanPane(
            onSearch = onSearch,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        )
    }
}

/**
 * The reusable OCR **snapshot** pane (no Scaffold), used both as the standalone screen and inline in
 * the scan screen's "Front" mode. Aim → tap **Capture** → the recognised title freezes into an
 * editable field → trim → **Search**.
 *
 * The live candidate *is* shown while aiming: without it there's no way to tell whether the camera
 * is reading anything, and a silent failure looks identical to a broken feature. Recomposition is
 * bounded because the candidate is only re-published when the guess actually changes.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FrontScanPane(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    // Best title guess from the most recent frame that read anything at all. Never blanked by a
    // momentarily bad frame, so Capture stays available once we've seen the title.
    var candidate by remember { mutableStateOf("") }
    // Null while aiming; a snapshot string once captured (switches to the review/edit state).
    var captured by remember { mutableStateOf<String?>(null) }

    // Front is the default scan mode, so ask for the camera instead of silently dropping the user
    // into the type-it-yourself fallback on first launch. Once per pane — no dialog spam.
    var permissionRequested by remember { mutableStateOf(false) }
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (!cameraPermission.status.isGranted && !permissionRequested) {
            permissionRequested = true
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            !cameraPermission.status.isGranted -> ManualFallback(
                onEnableCamera = { cameraPermission.launchPermissionRequest() },
                onSearch = onSearch,
            )

            captured == null -> {
                TextCamera(
                    onLines = { lines ->
                        val guess = bestTitleGuess(lines)
                        if (guess.isNotBlank() && guess != candidate) candidate = guess
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    if (candidate.isBlank()) {
                        "Aim at the title — fill the frame with the cover and hold steady."
                    } else {
                        "Reading: $candidate"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    // Guard: capturing a blank guess used to strand the user on the review step with
                    // an empty field and a disabled Search button.
                    onClick = { if (candidate.isNotBlank()) captured = candidate },
                    enabled = candidate.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Text("  Capture")
                }
            }

            else -> CapturedReview(
                initial = captured.orEmpty(),
                onRescan = {
                    // Clear the guess too, or a rescan can "capture" the previous item's title.
                    candidate = ""
                    captured = null
                },
                onSearch = onSearch,
            )
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
    var query by remember(initial) { mutableStateOf(initial) }
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

@Composable
private fun TextCamera(
    onLines: (List<OcrLine>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // OCR on a worker thread: the frames are ~1440x1080 now, which is far too much work to hang off
    // the main executor like the barcode path does.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val mainExecutor = ContextCompat.getMainExecutor(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    // ML Kit wants >= 1280x720 for text recognition and glyphs at least ~16px tall.
                    // CameraX defaults ImageAnalysis to 640x480, at which a cover title held at
                    // arm's length is a handful of pixels and simply never resolves — this was why
                    // Front mode read nothing while Barcode mode was fine. 4:3 to match the preview.
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1440, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build()

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, TextRecognitionAnalyzer(onLines)) }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }, mainExecutor)
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    // Stop the camera/analysis when this leaves the composition (e.g. after Capture).
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            analysisExecutor.shutdown()
        }
    }
}
