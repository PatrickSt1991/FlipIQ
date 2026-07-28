package nl.madebypatrick.flipiq.ui.haul

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.data.source.engine.HaulItem
import nl.madebypatrick.flipiq.data.source.engine.HaulService
import nl.madebypatrick.flipiq.data.source.engine.SCREENSHOT_MAX_DIM
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.ui.util.readImageAsJpeg
import javax.inject.Inject

/** A haul item after Profit-Mode triage: is it worth grabbing, and what's the most you'd pay? */
data class TriagedItem(
    val title: String,
    val value: Money?,
    val maxBuy: Money?,
    val interesting: Boolean,
    val imageUrl: String?,
)

@HiltViewModel
class HaulViewModel @Inject constructor(
    private val service: HaulService,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    var scanned by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var items by mutableStateOf<List<TriagedItem>>(emptyList())
        private set

    /** True when a picked/shared image couldn't be decoded — needs its own "try another" message. */
    var unreadable by mutableStateOf(false)
        private set

    fun scan(jpeg: ByteArray, rotationDegrees: Int) = start { service.scan(jpeg, rotationDegrees) }

    /** Price a pile from a picked/shared image; [uri] failing to decode surfaces as [unreadable]. */
    fun scanImage(uri: Uri) = start {
        val jpeg = readImageAsJpeg(context, uri, SCREENSHOT_MAX_DIM)
        if (jpeg == null) {
            unreadable = true
            emptyList()
        } else {
            // The loader already rotated it upright, so pass rotationDegrees = 0.
            service.scan(jpeg, rotationDegrees = 0, maxDim = SCREENSHOT_MAX_DIM)
        }
    }

    /** Shared in-flight bookkeeping + Profit-Mode triage for both the camera and image paths. */
    private fun start(fetch: suspend () -> List<HaulItem>) {
        if (loading) return
        loading = true
        scanned = true
        unreadable = false
        items = emptyList()
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val raw = fetch()
            items = raw
                .map { triage(it, settings) }
                // Grabs first, then by value.
                .sortedWith(
                    compareByDescending<TriagedItem> { it.interesting }
                        .thenByDescending { it.value ?: Money.ofCents(0) },
                )
            loading = false
        }
    }

    fun reset() {
        scanned = false
        loading = false
        unreadable = false
        items = emptyList()
    }

    /**
     * Same Profit-Mode maths the engine uses for a single item: net the resale of fees + shipping,
     * then take the highest buy price that still clears both the minimum profit and the minimum ROI.
     * "Interesting" = the value clears the ignore-below floor and there's room to buy and still profit.
     */
    private fun triage(item: HaulItem, s: ProfitSettings): TriagedItem {
        val value = item.value ?: return TriagedItem(item.title, null, null, false, item.imageUrl)
        val feeFraction = if (s.includeFees) s.marketplaceFee else 0.0
        val shipping = if (s.includeShipping) s.shippingCost.euros else 0.0
        val net = value.euros * (1.0 - feeFraction) - shipping
        val byProfit = net - s.minProfit.euros
        val byRoi = net / (1.0 + s.minRoi)
        val maxBuyEuros = minOf(byProfit, byRoi)
        val interesting = value >= s.ignoreBelow && maxBuyEuros > 0.0
        return TriagedItem(
            title = item.title,
            value = value,
            maxBuy = if (maxBuyEuros > 0.0) Money.ofEuros(maxBuyEuros) else null,
            interesting = interesting,
            imageUrl = item.imageUrl,
        )
    }
}
