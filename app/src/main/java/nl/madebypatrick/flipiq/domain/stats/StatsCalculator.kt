package nl.madebypatrick.flipiq.domain.stats

import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.InventorySummary
import nl.madebypatrick.flipiq.domain.model.ScanRecord

/** Aggregate figures for the statistics dashboard. */
data class FlipStats(
    val totalScans: Int,
    val averageDealScore: Int,
    /** Count of scans in each Deal Score bucket, always covering every tier (0 when none). */
    val tierBreakdown: Map<DealTier, Int>,
    val inventory: InventorySummary,
    /** Highest-potential inventory items, best first. */
    val topFlips: List<InventoryItem>,
) {
    companion object {
        val EMPTY = FlipStats(
            totalScans = 0,
            averageDealScore = 0,
            tierBreakdown = DealTier.entries.associateWith { 0 },
            inventory = InventorySummary.from(emptyList()),
            topFlips = emptyList(),
        )
    }
}

/** Pure, testable roll-up of scan history + inventory into [FlipStats]. */
object StatsCalculator {

    fun compute(
        history: List<ScanRecord>,
        inventory: List<InventoryItem>,
        topFlipsLimit: Int = 5,
    ): FlipStats {
        val averageScore = if (history.isEmpty()) 0 else history.sumOf { it.dealScore } / history.size

        // Start from zero for every tier so the UI can render a complete breakdown.
        val breakdown = DealTier.entries.associateWith { 0 }.toMutableMap()
        history.forEach { breakdown[it.tier] = (breakdown[it.tier] ?: 0) + 1 }

        val topFlips = inventory
            .sortedByDescending { it.projectedProfit.cents }
            .take(topFlipsLimit)

        return FlipStats(
            totalScans = history.size,
            averageDealScore = averageScore,
            tierBreakdown = breakdown,
            inventory = InventorySummary.from(inventory),
            topFlips = topFlips,
        )
    }
}
