package nl.madebypatrick.flipiq.domain.model

/**
 * A watch on an item: notify the user when it can be bought at or below [targetPrice].
 * [lastNotifiedAt] throttles repeat notifications for the same alert.
 */
data class PriceAlert(
    val id: Long = 0,
    val barcode: String,
    val title: String,
    val targetPrice: Money,
    val active: Boolean = true,
    val createdAt: Long,
    val lastNotifiedAt: Long? = null,
)
