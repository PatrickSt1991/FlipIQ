package nl.madebypatrick.flipiq.ui.scan

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * CameraX analyzer that runs ML Kit text recognition on each frame and reports the recognised lines
 * *with their on-screen height*, so [bestTitleGuess] can pick out the visually largest text (the
 * title) instead of the longest string.
 */
class TextRecognitionAnalyzer(
    private val onLines: (List<OcrLine>) -> Unit,
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { onLines(it.toOcrLines()) }
            .addOnCompleteListener { imageProxy.close() }
    }
}

/**
 * Flatten an ML Kit result into sized lines in reading order, dropping anything we can't measure.
 * Bounding boxes are already in upright coordinates because the rotation is passed to [InputImage],
 * so [android.graphics.Rect.height] is the glyph height the user sees.
 */
internal fun Text.toOcrLines(): List<OcrLine> =
    textBlocks.flatMap { it.lines }.mapNotNull { line ->
        val height = line.boundingBox?.height() ?: return@mapNotNull null
        val text = line.text.trim()
        if (text.isBlank()) null else OcrLine(text, height)
    }
