package nl.madebypatrick.flipiq.ui.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A CameraX back-camera preview that **reliably releases the camera** when it leaves composition —
 * so other apps can use the camera afterwards.
 *
 * Bind and unbind happen in one [DisposableEffect] against the same provider, with a `disposed`
 * guard: if the composable leaves before the (async) provider resolves, we skip binding entirely.
 * The previous code bound in an AndroidView factory callback and unbound in a separate effect, so a
 * quick navigate-away could bind the camera *after* dispose and never release it — locking the
 * camera for other apps until the process died.
 *
 * [extraUseCases] is invoked once at bind time; return any [ImageCapture]/[ImageAnalysis] to bind
 * alongside the preview.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    extraUseCases: () -> List<UseCase> = { emptyList() },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                *extraUseCases().toTypedArray(),
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            runCatching { future.get().unbindAll() }
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}
