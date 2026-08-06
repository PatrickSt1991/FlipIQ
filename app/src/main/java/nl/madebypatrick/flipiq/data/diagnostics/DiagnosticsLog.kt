package nl.madebypatrick.flipiq.data.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** One diagnostics line: when it happened + what happened. */
data class LogEntry(val timeMillis: Long, val message: String)

/**
 * Lightweight in-app diagnostics ring buffer — so we can see exactly what a scan did (which source
 * resolved a barcode, or that nothing did) without needing adb/logcat. Observe [entries] in the
 * Diagnostics screen. Global on purpose: it's written from data-layer classes that aren't all in the
 * Hilt graph, and it holds only ephemeral strings.
 */
object DiagnosticsLog {
    private const val MAX = 200
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries

    fun log(message: String) {
        _entries.update { (it + LogEntry(System.currentTimeMillis(), message)).takeLast(MAX) }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
