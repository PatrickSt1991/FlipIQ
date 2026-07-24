package nl.madebypatrick.flipiq.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CollectionTest {

    private fun item(
        buy: Double,
        resale: Double,
        status: InventoryStatus = InventoryStatus.IN_STOCK,
        sold: Double? = null,
    ) = InventoryItem(
        id = 1,
        barcode = "x",
        title = "item",
        buyPrice = Money.ofEuros(buy),
        estimatedResale = Money.ofEuros(resale),
        boughtAt = 0,
        status = status,
        soldPrice = sold?.let { Money.ofEuros(it) },
    )

    @Test
    fun `realized profit is null until the item is sold`() {
        assertThat(item(buy = 5.0, resale = 12.0).realizedProfit).isNull()
    }

    @Test
    fun `realized profit is sold price minus buy price`() {
        val sold = item(buy = 5.0, resale = 12.0, status = InventoryStatus.SOLD, sold = 11.0)
        assertThat(sold.realizedProfit).isEqualTo(Money.ofEuros(6.0))
    }

    @Test
    fun `projected profit falls back to the resale estimate while unsold`() {
        assertThat(item(buy = 5.0, resale = 12.0).projectedProfit).isEqualTo(Money.ofEuros(7.0))
    }

    @Test
    fun `projected profit uses the actual sold price once sold`() {
        val sold = item(buy = 5.0, resale = 12.0, status = InventoryStatus.SOLD, sold = 9.0)
        assertThat(sold.projectedProfit).isEqualTo(Money.ofEuros(4.0))
    }

    @Test
    fun `summary aggregates stock, sales and profit`() {
        val items = listOf(
            item(buy = 5.0, resale = 12.0), // in stock, projected +7
            item(buy = 8.0, resale = 20.0), // in stock, projected +12
            item(buy = 4.0, resale = 10.0, status = InventoryStatus.SOLD, sold = 15.0), // sold, realized +11
        )
        val summary = InventorySummary.from(items)

        assertThat(summary.itemsInStock).isEqualTo(2)
        assertThat(summary.itemsSold).isEqualTo(1)
        assertThat(summary.capitalInStock).isEqualTo(Money.ofEuros(13.0)) // 5 + 8
        assertThat(summary.realizedProfit).isEqualTo(Money.ofEuros(11.0)) // 15 - 4
        assertThat(summary.projectedProfit).isEqualTo(Money.ofEuros(30.0)) // 7 + 12 + 11
    }
}
