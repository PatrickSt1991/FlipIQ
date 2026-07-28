package nl.madebypatrick.flipiq.ui.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import nl.madebypatrick.flipiq.R
import nl.madebypatrick.flipiq.ui.util.rememberImagePicker
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * "Take a photo of the game, get the game" — the front-scan pane. Snaps the cover with CameraX and
 * sends the JPEG to the engine's vision model, which returns the title (even for stylised logos OCR
 * can't read). A typed fallback is always available for when there's no network or no match.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AiScanPane(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var message by remember { mutableStateOf<String?>(null) }

    // Picker must be remembered unconditionally — it's used in both the camera and no-camera branches.
    val pickImage = rememberImagePicker { uri ->
        message = null
        viewModel.identifyImage(uri) { title, unreadable ->
            if (title != null) {
                onSearch(title)
            } else {
                message = context.getString(
                    if (unreadable) R.string.image_unreadable else R.string.ai_not_identified,
                )
            }
        }
    }

    // Front is the default mode — ask for the camera once instead of dumping into the typed fallback.
    var permissionRequested by remember { mutableStateOf(false) }
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (!cameraPermission.status.isGranted && !permissionRequested) {
            permissionRequested = true
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!cameraPermission.status.isGranted) {
            Button(
                onClick = { cameraPermission.launchPermissionRequest() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.enable_camera)) }
            // A screenshot is a complete flow with no camera at all — offer it here, not just a dead
            // end into the typed fallback.
            ScreenshotButton(
                busy = viewModel.identifying,
                onClick = pickImage,
                modifier = Modifier.fillMaxWidth(),
            )
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            TypeItFallback(onSearch = onSearch)
            return@Column
        }

        CaptureCamera(
            imageCapture = imageCapture,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)),
        )
        Text(
            if (viewModel.identifying) stringResource(R.string.ai_identifying) else stringResource(R.string.ai_aim),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            enabled = !viewModel.identifying,
            onClick = {
                message = null
                imageCapture.takePicture(
                    mainExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val jpeg = image.toJpegBytes()
                            val rotation = image.imageInfo.rotationDegrees
                            image.close()
                            viewModel.identify(jpeg, rotation) { title ->
                                if (title != null) {
                                    onSearch(title)
                                } else {
                                    message = context.getString(R.string.ai_not_identified)
                                }
                            }
                        }

                        override fun onError(exc: ImageCaptureException) {
                            message = context.getString(R.string.ai_camera_error)
                        }
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.identifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Text(stringResource(R.string.ai_identify_game))
            }
        }
        ScreenshotButton(
            busy = viewModel.identifying,
            onClick = pickImage,
            modifier = Modifier.fillMaxWidth(),
        )
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        TypeItFallback(onSearch = onSearch)
    }
}

/**
 * Pick an image from the gallery instead of using the camera. `internal` so [HaulScreen] reuses the
 * exact same button. Outlined to sit visually below the primary capture/identify action.
 */
@Composable
internal fun ScreenshotButton(busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, enabled = !busy, modifier = modifier) {
        Icon(Icons.Default.Image, contentDescription = null)
        Text(stringResource(R.string.pick_screenshot))
    }
}

/** ImageCapture in JPEG mode packs the whole file into the first plane's buffer. */
private fun ImageProxy.toJpegBytes(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

@Composable
private fun TypeItFallback(onSearch: (String) -> Unit) {
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

@Composable
private fun CaptureCamera(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
) {
    CameraPreview(modifier = modifier, extraUseCases = { listOf(imageCapture) })
}
