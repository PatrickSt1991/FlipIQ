package nl.madebypatrick.flipiq.ui.share

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/** What another app handed us via *Share*. */
sealed interface SharedItem {
    data class Image(val uri: Uri) : SharedItem
    data class Title(val value: String) : SharedItem
    data class Barcode(val value: String) : SharedItem
}

/**
 * Map an incoming share [Intent] to a [SharedItem], or null for a normal launcher start.
 *
 * An attached image wins over text (a shared screenshot is unambiguous; the accompanying text is
 * often just "Shared from …"). `ACTION_SEND_MULTIPLE` keeps only the first image — `/haul` prices one
 * photo per call.
 */
fun Intent.toSharedItem(): SharedItem? = when (action) {
    Intent.ACTION_SEND -> {
        val image = IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
        // Nested-class constructor via ?.let (a bound reference like ::Image is fragile here).
        image?.let { SharedItem.Image(it) } ?: sharedTextItem()
    }

    Intent.ACTION_SEND_MULTIPLE ->
        IntentCompat.getParcelableArrayListExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            ?.firstOrNull()
            ?.let { SharedItem.Image(it) }

    else -> null
}

private fun Intent.sharedTextItem(): SharedItem? {
    val subject = getStringExtra(Intent.EXTRA_SUBJECT)?.trim()?.takeIf { it.isUsableSubject() }
    val text = getStringExtra(Intent.EXTRA_TEXT)?.trim()

    // A bare UPC/EAN in the body goes straight to a barcode lookup.
    text?.let { sharedBarcode(it) }?.let { return SharedItem.Barcode(it) }

    // Subject first (mail-style shares put the clean title there and filler in the body), then body.
    val title = subject?.let { sharedTitle(it) } ?: text?.let { sharedTitle(it) }
    return title?.let { SharedItem.Title(it) }
}

/**
 * Reject subjects that are obviously the app talking rather than a listing/page title. A subject
 * that merely contains a marketplace name (e.g. a browser page title `… | Marktplaats`) is fine —
 * the suffix is trimmed downstream.
 */
private fun String.isUsableSubject(): Boolean {
    if (length < 6) return false
    val lower = lowercase()
    val chatter = listOf("shared with you", "check out", "invitation", "gedeeld met", "uitnodiging")
    return chatter.none { lower.contains(it) }
}
