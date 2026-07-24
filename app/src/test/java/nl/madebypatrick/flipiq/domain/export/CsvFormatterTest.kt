package nl.madebypatrick.flipiq.domain.export

import com.google.common.truth.Truth.assertThat
import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.InventoryStatus
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ScanRecord
import nl.madebypatrick.flipiq.domain.model.SellSpeed
import org.junit.Test

class CsvFormatterTest {

    @Test
    fun `inventory csv has a header and one row per item`() {
        val items = listOf(
            InventoryItem(
                id = 1, barcode = "123", title = "Halo 3", buyPrice = Money.ofEuros(5.0),
                estimatedResale = Money.ofEuros(12.0), boughtAt = 0L,
                status = InventoryStatus.SOLD, soldPrice = Money.ofEuros(11.0), soldAt = 0L,
            ),
        )
        val csv = CsvFormatter.inventoryCsv(items)
        val lines = csv.split("\n")

        assertThat(lines).hasSize(2)
        assertThat(lines[0]).startsWith("Title,Barcode,Buy Price")
        assertThat(lines[1]).contains("Halo 3")
        assertThat(lines[1]).contains("5.00")   // buy price
        assertThat(lines[1]).contains("11.00")  // sold price
        assertThat(lines[1]).contains("6.00")   // realized profit
        assertThat(lines[1]).contains("1970-01-01 00:00") // UTC epoch
    }

    @Test
    fun `fields with commas and quotes are escaped per RFC-4180`() {
        val records = listOf(
            ScanRecord(
                id = 1, barcode = "9", title = "Rock, Paper & \"Scissors\"", category = null,
                dealScore = 80, tier = DealTier.GREAT_DEAL, estimatedResale = Money.ofEuros(10.0),
                recommendedBuy = Money.ofEuros(6.0), sellSpeed = SellSpeed.FAST, scannedAt = 0L,
            ),
        )
        val row = CsvFormatter.historyCsv(records).split("\n")[1]
        // The title contains a comma and quotes → wrapped in quotes with interior quotes doubled.
        assertThat(row).contains("\"Rock, Paper & \"\"Scissors\"\"\"")
    }

    @Test
    fun `empty collections still emit the header row`() {
        assertThat(CsvFormatter.inventoryCsv(emptyList()).split("\n")).hasSize(1)
        assertThat(CsvFormatter.historyCsv(emptyList()).split("\n")).hasSize(1)
    }
}
