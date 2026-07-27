package nl.madebypatrick.flipiq.ui.haul

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.ui.scan.CameraPreview

/**
 * Haul scan — photograph a pile of games/DVDs; the engine lists them and prices each, and Profit
 * Mode flags what's worth grabbing. Tap an item for its full price + Deal Score. (issue #48)
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HaulScreen(
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit,
    viewModel: HaulViewModel = hiltViewModel(),
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.haul_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !cameraPermission.status.isGranted -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text(stringResource(R.string.enable_camera))
                    }
                }

                viewModel.loading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.haul_reading),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                viewModel.scanned -> ResultsList(
                    viewModel = viewModel,
                    onOpenGame = onOpenGame,
                    onScanAgain = viewModel::reset,
                )

                else -> CaptureView(
                    imageCapture = imageCapture,
                    onCapture = {
                        imageCapture.takePicture(
                            mainExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bytes = image.toJpegBytes()
                                    val rotation = image.imageInfo.rotationDegrees
                                    image.close()
                                    viewModel.scan(bytes, rotation)
                                }

                                override fun onError(exc: ImageCaptureException) {}
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CaptureView(imageCapture: ImageCapture, onCapture: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CaptureCamera(
            imageCapture = imageCapture,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp)),
        )
        Text(stringResource(R.string.haul_aim), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.haul_scan_button))
        }
    }
}

@Composable
private fun ResultsList(
    viewModel: HaulViewModel,
    onOpenGame: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (viewModel.items.isEmpty()) {
            Text(
                stringResource(R.string.haul_empty),
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.items) { item -> HaulRow(item, onOpenGame) }
            }
        }
        OutlinedButton(
            onClick = onScanAgain,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text(stringResource(R.string.haul_scan_again)) }
    }
}

@Composable
private fun HaulRow(item: TriagedItem, onOpenGame: (String) -> Unit) {
    val grab = item.interesting
    val accent = if (grab) Color(0xFF2E7D32) else LocalContentColor.current.copy(alpha = 0.5f)
    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = {
            val value = item.value?.toString() ?: stringResource(R.string.haul_no_value)
            val maxBuy = item.maxBuy?.let { " · " + stringResource(R.string.haul_max_buy, it.toString()) } ?: ""
            Text(value + maxBuy)
        },
        leadingContent = {
            Icon(
                if (grab) Icons.Default.CheckCircle else Icons.Default.RemoveCircleOutline,
                contentDescription = stringResource(if (grab) R.string.haul_grab else R.string.haul_skip),
                tint = accent,
            )
        },
        colors = ListItemDefaults.colors(),
        modifier = Modifier.clickable { onOpenGame(item.title) },
    )
}

/** ImageCapture JPEG mode packs the whole file into the first plane's buffer. */
private fun ImageProxy.toJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

@Composable
private fun CaptureCamera(imageCapture: ImageCapture, modifier: Modifier = Modifier) {
    CameraPreview(modifier = modifier, extraUseCases = { listOf(imageCapture) })
}
