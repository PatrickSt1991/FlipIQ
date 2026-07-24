package nl.madebypatrick.flipiq.domain.export

import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.ScanRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Turns collection data into CSV text. Pure and deterministic (timestamps formatted in UTC), so the
 * output can be unit-tested; the Android side only handles writing the string to a file and sharing.
 */
object CsvFormatter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    fun inventoryCsv(items: List<InventoryItem>): String {
        val header = listOf(
            "Title", "Barcode", "Buy Price", "Estimated Resale", "Status",
            "Sold Price", "Projected Profit", "Realized Profit", "Bought At (UTC)",
        )
        val rows = items.map { i ->
            listOf(
                i.title,
                i.barcode,
                euros(i.buyPrice.euros),
                euros(i.estimatedResale.euros),
                i.status.name,
                i.soldPrice?.let { euros(it.euros) } ?: "",
                euros(i.projectedProfit.euros),
                i.realizedProfit?.let { euros(it.euros) } ?: "",
                dateFormat.format(Date(i.boughtAt)),
            )
        }
        return build(header, rows)
    }

    fun historyCsv(records: List<ScanRecord>): String {
        val header = listOf(
            "Title", "Barcode", "Category", "Deal Score", "Tier",
            "Estimated Resale", "Recommended Buy", "Sell Speed", "Scanned At (UTC)",
        )
        val rows = records.map { r ->
            listOf(
                r.title,
                r.barcode,
                r.category ?: "",
                r.dealScore.toString(),
                r.tier.name,
                euros(r.estimatedResale.euros),
                euros(r.recommendedBuy.euros),
                r.sellSpeed.name,
                dateFormat.format(Date(r.scannedAt)),
            )
        }
        return build(header, rows)
    }

    private fun build(header: List<String>, rows: List<List<String>>): String =
        (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") { escape(it) } }

    private fun euros(value: Double) = String.format(Locale.US, "%.2f", value)

    /** RFC-4180 escaping: quote fields containing a comma, quote or newline; double interior quotes. */
    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
