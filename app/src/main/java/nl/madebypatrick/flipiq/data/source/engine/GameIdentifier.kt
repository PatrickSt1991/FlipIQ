package nl.madebypatrick.flipiq.data.source.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Default longest-side cap for a camera photo — a cover title is legible at ~1024px. */
const val PHOTO_MAX_DIM = 1024

/**
 * Higher cap for a listing screenshot: a screenshot is text, so squashing a 1080×2400 phone screen
 * to 1024 leaves the title ~15 px tall. Give it more room so the vision model can read it.
 */
const val SCREENSHOT_MAX_DIM = 1568

/**
 * Identifies a video game from a photo of its cover/case (or a screenshot). A vision model reads the
 * whole cover — art, characters, stylised logos — so it succeeds where on-device OCR fails on
 * distressed titles like FIFA Street. The heavy lifting (and the API key) live in the engine Worker;
 * the app just uploads the JPEG.
 */
interface GameIdentifier {
    /**
     * Returns a game title, or null if it can't be identified (never throws).
     * [rotationDegrees] is the capture rotation (from `ImageProxy.imageInfo.rotationDegrees`) so the
     * image can be sent upright. [maxDim] caps the longest side of the uploaded JPEG.
     */
    suspend fun identify(jpeg: ByteArray, rotationDegrees: Int = 0, maxDim: Int = PHOTO_MAX_DIM): String?
}

/** No-op: used when the engine isn't configured (identification unavailable). */
class NoopGameIdentifier : GameIdentifier {
    override suspend fun identify(jpeg: ByteArray, rotationDegrees: Int, maxDim: Int): String? = null
}

/** Posts the JPEG (downscaled, upright, base64) to the engine's `/identify` and returns the title. */
class EngineGameIdentifier(
    private val api: EngineApi,
    private val engineUrl: String,
    private val appKey: String,
) : GameIdentifier {
    override suspend fun identify(jpeg: ByteArray, rotationDegrees: Int, maxDim: Int): String? =
        withContext(Dispatchers.Default) {
            // A full-res phone photo is 3–5 MB; uploading that (and having the model chew through it)
            // is the bulk of the latency. A cover title is legible at ~1024px, so shrink first.
            val prepared = prepareForUpload(jpeg, rotationDegrees, maxDim)
            val b64 = Base64.encodeToString(prepared, Base64.NO_WRAP)
            val endpoint = engineUrl.trimEnd('/') + "/identify"
            runCatching { api.identify(endpoint, appKey, IdentifyRequest(image = b64)).title }
                .getOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }
}

/** Decode → rotate upright → scale so the longest side ≤ [maxDim] → re-encode JPEG. */
private const val JPEG_QUALITY = 85

internal fun prepareForUpload(jpeg: ByteArray, rotationDegrees: Int, maxDim: Int = PHOTO_MAX_DIM): ByteArray {
    // Cheap first pass: let the decoder subsample so we never hold the full-res bitmap in memory.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
    val (w, h) = bounds.outWidth to bounds.outHeight
    if (w <= 0 || h <= 0) return jpeg

    // Subsample on the *longer* side: a tall narrow image (a stitched screenshot) must shrink too —
    // testing both axes with && left it at full resolution until the scale step.
    var sample = 1
    while (maxOf(w, h) / (sample * 2) >= maxDim) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(
        jpeg, 0, jpeg.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return jpeg

    // Exact scale to maxDim on the longer side, plus the capture rotation, in one matrix.
    val longer = maxOf(decoded.width, decoded.height)
    val scale = if (longer > maxDim) maxDim.toFloat() / longer else 1f
    val matrix = Matrix().apply {
        if (scale != 1f) postScale(scale, scale)
        if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
    }
    val out = if (scale != 1f || rotationDegrees != 0) {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    } else {
        decoded
    }

    return ByteArrayOutputStream().use { stream ->
        out.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        stream.toByteArray()
    }
}
