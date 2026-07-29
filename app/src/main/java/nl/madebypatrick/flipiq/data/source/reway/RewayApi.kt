package nl.madebypatrick.flipiq.data.source.reway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Reway's Shopify storefront search API. Both stores (`rewayverkopen.nl` inkoop, `reway.nl` retail)
 * expose the identical endpoints, so a single interface serves both — the host is passed per call via
 * [Url], the way [nl.madebypatrick.flipiq.data.source.engine.EngineApi] does, since one Retrofit
 * baseUrl can't cover two hosts.
 *
 * Shopify's predictive search (`/search/suggest.json`) returns up to ~10 candidates with `vendor`
 * (the platform) and a price range, but **no variants** — condition-level pricing needs a second
 * `/products/<handle>.js` call, done lazily and only when the user asks for the breakdown.
 * See https://shopify.dev/docs/api/ajax/reference/predictive-search.
 *
 * All DTO fields are nullable with defaults and the JSON parser ignores unknown keys (see
 * [nl.madebypatrick.flipiq.di.NetworkModule.provideJson]) — Shopify returns several KB of marketing
 * `body` HTML and fields we never read.
 *
 * The bracketed query params (`resources[type]`, `resources[limit]`) are declared as ordinary
 * [Query] names; Retrofit/OkHttp percent-encode the brackets to `%5B`/`%5D`, which Shopify accepts.
 */
interface RewayApi {

    @GET
    suspend fun suggest(
        @Url url: String,
        @Query("q") q: String,
        @Query("resources[type]") type: String = "product",
        @Query("resources[limit]") limit: Int = 6,
        @Header("User-Agent") userAgent: String = REWAY_USER_AGENT,
    ): RewaySuggestResponse

    companion object {
        /** Repo-identifying agent so Reway can see who's calling; also set by an OkHttp interceptor. */
        const val REWAY_USER_AGENT = "FlipIQ/1.0 (+https://github.com/PatrickSt1991/FlipIQ)"
    }
}

@Serializable
data class RewaySuggestResponse(
    @SerialName("resources") val resources: RewayResources? = null,
) {
    /** Flattened accessor for the deeply-nested product list. */
    val products: List<RewayProductDto>
        get() = resources?.results?.products ?: emptyList()
}

@Serializable
data class RewayResources(
    @SerialName("results") val results: RewayResults? = null,
)

@Serializable
data class RewayResults(
    @SerialName("products") val products: List<RewayProductDto> = emptyList(),
)

/**
 * One product from `suggest.json`.
 *
 * - [vendor] is the **platform** (`Playstation 4`, `LEGO`, `VTech`) — the key that makes matching
 *   viable at all (§4).
 * - Prices are decimal strings here (`"23.95"`); use [priceMax], not [price] — the verkopen store
 *   labels its figure "Maximaal" and the range's top is the headline (§2).
 * - [type] is the category here (`Games`, `Bouwsets`); `products.json` calls the same field
 *   `product_type`, hence both are accepted into [type].
 * - [url] is relative and carries `_`-prefixed tracking params to be stripped (§2).
 * - [variants] is always empty in `suggest.json`.
 */
@Serializable
data class RewayProductDto(
    @SerialName("title") val title: String? = null,
    @SerialName("handle") val handle: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("price") val price: String? = null,
    @SerialName("price_min") val priceMin: String? = null,
    @SerialName("price_max") val priceMax: String? = null,
    @SerialName("available") val available: Boolean = false,
    @SerialName("vendor") val vendor: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("product_type") val productType: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
) {
    /** `type` (suggest.json/.js) or `product_type` (products.json) — one logical category field. */
    val category: String? get() = type ?: productType
}
