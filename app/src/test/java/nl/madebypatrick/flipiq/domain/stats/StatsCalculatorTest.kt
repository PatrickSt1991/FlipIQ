package nl.madebypatrick.flipiq.domain.stats

import com.google.common.truth.Truth.assertThat
import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ScanRecord
import nl.madebypatrick.flipiq.domain.model.SellSpeed
import org.junit.Test

class StatsCalculatorTest {

    private fun scan(score: Int, tier: DealTier) = ScanRecord(
        barcode = "x", title = "item", category = null, dealScore = score, tier = tier,
        estimatedResale = Money.ofEuros(10.0), recommendedBuy = Money.ofEuros(5.0),
        sellSpeed = SellSpeed.FAST, scannedAt = 0L,
    )

    private fun item(buy: Double, resale: Double, id: Long) = InventoryItem(
        id = id, barcode = "b$id", title = "item$id", buyPrice = Money.ofEuros(buy),
        estimatedResale = Money.ofEuros(resale), boughtAt = 0L,
    )

    @Test
    fun `empty inputs give zeroed stats with every tier present`() {
        val stats = StatsCalculator.compute(emptyList(), emptyList())
        assertThat(stats.totalScans).isEqualTo(0)
        assertThat(stats.averageDealScore).isEqualTo(0)
        assertThat(stats.tierBreakdown.keys).containsExactlyElementsIn(DealTier.entries)
        assertThat(stats.tierBreakdown.values.all { it == 0 }).isTrue()
    }

    @Test
    fun `average deal score is the mean over history`() {
        val stats = StatsCalculator.compute(
            listOf(scan(90, DealTier.BUY_IMMEDIATELY), scan(60, DealTier.FAIR_PRICE), scan(30, DealTier.LOW_PROFIT)),
            emptyList(),
        )
        assertThat(stats.averageDealScore).isEqualTo(60)
    }

    @Test
    fun `tier breakdown counts each tier`() {
        val stats = StatsCalculator.compute(
            listOf(
                scan(95, DealTier.BUY_IMMEDIATELY),
                scan(92, DealTier.BUY_IMMEDIATELY),
                scan(10, DealTier.SKIP),
            ),
            emptyList(),
        )
        assertThat(stats.tierBreakdown[DealTier.BUY_IMMEDIATELY]).isEqualTo(2)
        assertThat(stats.tierBreakdown[DealTier.SKIP]).isEqualTo(1)
        assertThat(stats.tierBreakdown[DealTier.GREAT_DEAL]).isEqualTo(0)
    }

    @Test
    fun `top flips are ordered by projected profit and capped`() {
        val items = listOf(
            item(buy = 5.0, resale = 8.0, id = 1),   // +3
            item(buy = 5.0, resale = 25.0, id = 2),  // +20
            item(buy = 5.0, resale = 15.0, id = 3),  // +10
        )
        val stats = StatsCalculator.compute(emptyList(), items, topFlipsLimit = 2)
        assertThat(stats.topFlips.map { it.id }).containsExactly(2L, 3L).inOrder()
    }

    @Test
    fun `inventory summary is rolled into the stats`() {
        val stats = StatsCalculator.compute(emptyList(), listOf(item(buy = 5.0, resale = 12.0, id = 1)))
        assertThat(stats.inventory.itemsInStock).isEqualTo(1)
        assertThat(stats.inventory.capitalInStock).isEqualTo(Money.ofEuros(5.0))
    }
}
