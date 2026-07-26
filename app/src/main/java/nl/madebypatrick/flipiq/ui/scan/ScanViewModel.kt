package nl.madebypatrick.flipiq.ui.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.source.engine.GameIdentifier
import javax.inject.Inject

/** Holds the "identify a game from a snapshot" call and its in-flight state for the scan screen. */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val identifier: GameIdentifier,
) : ViewModel() {

    /** True while a snapshot is being identified — disables the button and shows a spinner. */
    var identifying by mutableStateOf(false)
        private set

    /** Identify the captured JPEG; [onResult] gets the title, or null if it couldn't be identified. */
    fun identify(jpeg: ByteArray, onResult: (String?) -> Unit) {
        if (identifying) return
        identifying = true
        viewModelScope.launch {
            val title = identifier.identify(jpeg)
            identifying = false
            onResult(title)
        }
    }
}
