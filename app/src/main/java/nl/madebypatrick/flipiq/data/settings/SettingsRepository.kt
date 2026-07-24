package nl.madebypatrick.flipiq.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ProfitSettings

/**
 * Persists the user's Profit Mode configuration in a Preferences DataStore and exposes it as a
 * [ProfitSettings] the engine consumes. Any unset key falls back to [ProfitSettings.DEFAULT].
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val MIN_PROFIT_CENTS = longPreferencesKey("min_profit_cents")
        val MIN_ROI = doublePreferencesKey("min_roi")
        val IGNORE_BELOW_CENTS = longPreferencesKey("ignore_below_cents")
        val MIN_SALES = intPreferencesKey("min_sales")
        val IGNORE_INCOMPLETE = booleanPreferencesKey("ignore_incomplete")
        val IGNORE_DAMAGED = booleanPreferencesKey("ignore_damaged")
        val PREFER_FAST = booleanPreferencesKey("prefer_fast_sellers")
        val INCLUDE_SHIPPING = booleanPreferencesKey("include_shipping")
        val INCLUDE_FEES = booleanPreferencesKey("include_fees")
        val MARKETPLACE_FEE = doublePreferencesKey("marketplace_fee")
        val SHIPPING_CENTS = longPreferencesKey("shipping_cents")
        val PRICECHARTING_TOKEN = stringPreferencesKey("pricecharting_token")
    }

    val settings: Flow<ProfitSettings> = dataStore.data.map { it.toSettings() }

    /** User-entered PriceCharting API token (empty when unset). */
    val priceChartingToken: Flow<String> = dataStore.data.map { it[Keys.PRICECHARTING_TOKEN] ?: "" }

    suspend fun setPriceChartingToken(token: String) {
        dataStore.edit { it[Keys.PRICECHARTING_TOKEN] = token.trim() }
    }

    suspend fun update(settings: ProfitSettings) {
        dataStore.edit { p ->
            p[Keys.MIN_PROFIT_CENTS] = settings.minProfit.cents
            p[Keys.MIN_ROI] = settings.minRoi
            p[Keys.IGNORE_BELOW_CENTS] = settings.ignoreBelow.cents
            p[Keys.MIN_SALES] = settings.minSales
            p[Keys.IGNORE_INCOMPLETE] = settings.ignoreIncomplete
            p[Keys.IGNORE_DAMAGED] = settings.ignoreDamaged
            p[Keys.PREFER_FAST] = settings.preferFastSellers
            p[Keys.INCLUDE_SHIPPING] = settings.includeShipping
            p[Keys.INCLUDE_FEES] = settings.includeFees
            p[Keys.MARKETPLACE_FEE] = settings.marketplaceFee
            p[Keys.SHIPPING_CENTS] = settings.shippingCost.cents
        }
    }

    private fun Preferences.toSettings(): ProfitSettings {
        val d = ProfitSettings.DEFAULT
        return ProfitSettings(
            minProfit = this[Keys.MIN_PROFIT_CENTS]?.let { Money(it) } ?: d.minProfit,
            minRoi = this[Keys.MIN_ROI] ?: d.minRoi,
            ignoreBelow = this[Keys.IGNORE_BELOW_CENTS]?.let { Money(it) } ?: d.ignoreBelow,
            minSales = this[Keys.MIN_SALES] ?: d.minSales,
            ignoreIncomplete = this[Keys.IGNORE_INCOMPLETE] ?: d.ignoreIncomplete,
            ignoreDamaged = this[Keys.IGNORE_DAMAGED] ?: d.ignoreDamaged,
            preferFastSellers = this[Keys.PREFER_FAST] ?: d.preferFastSellers,
            includeShipping = this[Keys.INCLUDE_SHIPPING] ?: d.includeShipping,
            includeFees = this[Keys.INCLUDE_FEES] ?: d.includeFees,
            marketplaceFee = this[Keys.MARKETPLACE_FEE] ?: d.marketplaceFee,
            shippingCost = this[Keys.SHIPPING_CENTS]?.let { Money(it) } ?: d.shippingCost,
        )
    }
}
