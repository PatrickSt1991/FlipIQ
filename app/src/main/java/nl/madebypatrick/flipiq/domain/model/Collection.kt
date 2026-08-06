package nl.madebypatrick.flipiq.domain.model

/** Where an inventory item is in its flip lifecycle. */
enum class InventoryStatus { IN_STOCK, LISTED, SOLD }

/** The two saved-item lists from the README's Collection: things you like, and things you want. */
enum class SavedList { FAVORITE, WISHLIST }

/** An item the user saved to their Favorites or Wishlist. */
data class SavedItem(
    val id: Long = 0,
    val barcode: String,
    val title: String,
    val list: SavedList,
    val savedAt: Long,
)

/** A single past scan, kept as history. */
data class ScanRecord(
    val id: Long = 0,
    val barcode: String,
    val title: String,
    val category: String?,
    val dealScore: Int,
    val tier: DealTier,
    val estimatedResale: Money,
    val recommendedBuy: Money,
    val sellSpeed: SellSpeed,
    val scannedAt: Long,
)

/** An item the user has bought to flip, tracked from purchase through sale. */
data class InventoryItem(
    val id: Long = 0,
    val barcode: String,
    val title: String,
    val buyPrice: Money,
    val estimatedResale: Money,
    val boughtAt: Long,
    val status: InventoryStatus = InventoryStatus.IN_STOCK,
    val soldPrice: Money? = null,
    val soldAt: Long? = null,
    val imageUrl: String? = null,
) {
    /** Profit already banked — only meaningful once the item is SOLD. */
    val realizedProfit: Money?
        get() = soldPrice?.let { it - buyPrice }

    /** Best current estimate of profit: realized if sold, otherwise projected from resale estimate. */
    val projectedProfit: Money
        get() = (soldPrice ?: estimatedResale) - buyPrice
}

/** Roll-up of the whole inventory for the profit tracker. */
data class InventorySummary(
    val itemsInStock: Int,
    val itemsSold: Int,
    val capitalInStock: Money,
    val realizedProfit: Money,
    val projectedProfit: Money,
) {
    companion object {
        fun from(items: List<InventoryItem>): InventorySummary {
            val inStock = items.filter { it.status != InventoryStatus.SOLD }
            val sold = items.filter { it.status == InventoryStatus.SOLD }
            return InventorySummary(
                itemsInStock = inStock.size,
                itemsSold = sold.size,
                capitalInStock = Money(inStock.sumOf { it.buyPrice.cents }),
                realizedProfit = Money(sold.sumOf { (it.realizedProfit ?: Money.ZERO).cents }),
                projectedProfit = Money(items.sumOf { it.projectedProfit.cents }),
            )
        }
    }
}
