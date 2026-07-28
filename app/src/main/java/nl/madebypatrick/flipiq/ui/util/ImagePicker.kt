package nl.madebypatrick.flipiq.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Remembers a launcher for the system photo picker and returns a `launch()` lambda. Uses
 * [ActivityResultContracts.PickVisualMedia] — the modern picker needs **no storage permission** on
 * any API level (system picker on 33+, an `OPEN_DOCUMENT` / `GET_CONTENT` fallback below), so there
 * is deliberately no `READ_MEDIA_IMAGES` request anywhere.
 */
@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicked)
    }
    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
}

/** JPEG re-encode quality — matches the engine upload pipeline. */
private const val JPEG_QUALITY = 85

/** Anything bigger than this is refused outright rather than risking an OOM decode. */
private const val MAX_PIXELS = 80_000_000L

/**
 * Reads [uri] and returns **upright JPEG bytes with the longest side ≤ [maxDim]**, or `null` on any
 * failure (revoked grant, corrupt file, unsupported codec, absurd size). Runs on [Dispatchers.IO].
 *
 * Screenshots are PNG and carry no EXIF; gallery photos are JPEG and do. We always re-encode to JPEG
 * (the engine is told `image/jpeg`) and apply the EXIF rotation + scale in a single matrix.
 */
suspend fun readImageAsJpeg(context: Context, uri: Uri, maxDim: Int): ByteArray? =
    withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // Cheap bounds-only pass so we never hold a full-res bitmap in memory.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return@runCatching null
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return@runCatching null
            if (w.toLong() * h.toLong() > MAX_PIXELS) return@runCatching null

            // Subsample on the *longer* side so a 1080×9000 stitched screenshot shrinks too.
            var sample = 1
            while (maxOf(w, h) / (sample * 2) >= maxDim) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            } ?: return@runCatching null

            val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0

            val longer = maxOf(decoded.width, decoded.height)
            val scale = if (longer > maxDim) maxDim.toFloat() / longer else 1f
            val matrix = Matrix().apply {
                if (scale != 1f) postScale(scale, scale)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
            val out = if (scale != 1f || rotation != 0) {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else {
                decoded
            }

            ByteArrayOutputStream().use { stream ->
                out.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                stream.toByteArray()
            }
        }.getOrNull()
    }
