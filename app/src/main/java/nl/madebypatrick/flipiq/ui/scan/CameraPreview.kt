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
 * Two rules keep it correct when a *second* preview appears while this one is still on screen (any
 * navigation between two camera screens: the outgoing destination stays in composition until the
 * transition finishes, so it disposes **after** the incoming one has bound):
 *  - Binding clears everything ([ProcessCameraProvider.unbindAll]) — CameraX allows only one active
 *    [androidx.lifecycle.LifecycleOwner], and two `Preview`s is not a supported surface combination.
 *  - Disposing unbinds only *our own* use cases. `unbindAll()` here used to tear down the incoming
 *    screen's freshly bound camera, leaving a frozen preview and an [ImageCapture] whose
 *    `takePicture` only ever reports `ERROR_INVALID_CAMERA`.
 *
 * The use cases are therefore built once, so dispose can name the same instances. [extraUseCases]
 * is invoked once, at first composition; return any [ImageCapture]/[ImageAnalysis] to bind alongside
 * the preview.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    extraUseCases: () -> List<UseCase> = { emptyList() },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val preview = remember { Preview.Builder().build() }
    val useCases = remember { (listOf<UseCase>(preview) + extraUseCases()).toTypedArray() }

    DisposableEffect(lifecycleOwner) {
        var disposed = false
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            // The listener only runs once the future is done, so get() returns without blocking.
            val resolved = future.get()
            provider = resolved
            preview.surfaceProvider = previewView.surfaceProvider
            resolved.unbindAll()
            resolved.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases)
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            // Never future.get() here: on the first camera launch the provider may still be
            // initialising, and blocking the main thread on it is an ANR.
            provider?.unbind(*useCases)
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}
