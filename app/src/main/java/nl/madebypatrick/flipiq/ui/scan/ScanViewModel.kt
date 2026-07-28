package nl.madebypatrick.flipiq.ui.scan

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.source.engine.GameIdentifier
import nl.madebypatrick.flipiq.data.source.engine.SCREENSHOT_MAX_DIM
import nl.madebypatrick.flipiq.ui.util.readImageAsJpeg
import javax.inject.Inject

/** Holds the "identify a game from a snapshot" call and its in-flight state for the scan screen. */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val identifier: GameIdentifier,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** True while a snapshot is being identified — disables the button and shows a spinner. */
    var identifying by mutableStateOf(false)
        private set

    /** Identify the captured JPEG; [onResult] gets the title, or null if it couldn't be identified. */
    fun identify(jpeg: ByteArray, rotationDegrees: Int, onResult: (String?) -> Unit) {
        if (identifying) return
        identifying = true
        viewModelScope.launch {
            val title = identifier.identify(jpeg, rotationDegrees)
            identifying = false
            onResult(title)
        }
    }

    /**
     * Identify a game from a picked/shared image [uri]. Decoding happens **inside** the [identifying]
     * window so the existing spinner covers it. Two failures are reported separately:
     * `unreadable = true` means the file couldn't be decoded (pick another); `title == null` with
     * `unreadable = false` means the engine found no game (type the title). They need different copy.
     */
    fun identifyImage(uri: Uri, onResult: (title: String?, unreadable: Boolean) -> Unit) {
        if (identifying) return
        identifying = true
        viewModelScope.launch {
            val jpeg = readImageAsJpeg(context, uri, SCREENSHOT_MAX_DIM)
            if (jpeg == null) {
                identifying = false
                onResult(null, true)
                return@launch
            }
            // The loader already rotated it upright, so pass rotationDegrees = 0.
            val title = identifier.identify(jpeg, rotationDegrees = 0, maxDim = SCREENSHOT_MAX_DIM)
            identifying = false
            onResult(title, false)
        }
    }
}
