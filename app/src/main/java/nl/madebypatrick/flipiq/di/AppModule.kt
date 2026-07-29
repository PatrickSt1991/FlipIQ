package nl.madebypatrick.flipiq.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import nl.madebypatrick.flipiq.BuildConfig
import nl.madebypatrick.flipiq.data.repository.PriceRepository
import nl.madebypatrick.flipiq.data.resolver.BarcodeResolver
import nl.madebypatrick.flipiq.data.resolver.CompositeBarcodeResolver
import nl.madebypatrick.flipiq.data.resolver.EanSearchApi
import nl.madebypatrick.flipiq.data.resolver.EanSearchResolver
import nl.madebypatrick.flipiq.data.resolver.NoopBarcodeResolver
import nl.madebypatrick.flipiq.data.resolver.OpenLibraryApi
import nl.madebypatrick.flipiq.data.resolver.OpenLibraryResolver
import nl.madebypatrick.flipiq.data.resolver.UpcItemDbApi
import nl.madebypatrick.flipiq.data.resolver.UpcItemDbResolver
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ShortcutOnlySource
import nl.madebypatrick.flipiq.data.source.engine.EngineApi
import nl.madebypatrick.flipiq.data.source.engine.EngineGameIdentifier
import nl.madebypatrick.flipiq.data.source.engine.EngineSource
import nl.madebypatrick.flipiq.data.source.engine.EngineHaulService
import nl.madebypatrick.flipiq.data.source.engine.EngineTopGamesService
import nl.madebypatrick.flipiq.data.source.engine.GameIdentifier
import nl.madebypatrick.flipiq.data.source.engine.HaulService
import nl.madebypatrick.flipiq.data.source.engine.NoopGameIdentifier
import nl.madebypatrick.flipiq.data.source.engine.NoopHaulService
import nl.madebypatrick.flipiq.data.source.engine.NoopTopGamesService
import nl.madebypatrick.flipiq.data.source.engine.TopGamesService
import nl.madebypatrick.flipiq.data.source.mock.EbaySoldSource
import nl.madebypatrick.flipiq.data.source.mock.PriceChartingSource
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingApi
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingSource as LivePriceChartingSource
import nl.madebypatrick.flipiq.data.source.reway.RewayApi
import nl.madebypatrick.flipiq.data.source.reway.RewaySource
import nl.madebypatrick.flipiq.data.source.reway.RewayThrottle
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.CurrencyConverter
import nl.madebypatrick.flipiq.domain.StaticCurrencyConverter
import nl.madebypatrick.flipiq.domain.engine.EngineConfig
import nl.madebypatrick.flipiq.domain.engine.FlipIQEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEngine(): FlipIQEngine = FlipIQEngine(EngineConfig.DEFAULT)

    @Provides
    @Singleton
    fun provideCurrencyConverter(): CurrencyConverter = StaticCurrencyConverter()

    /**
     * The active set of marketplace sources.
     *
     * **Real price data** comes from the FlipIQ Engine (Marktplaats + eBay, aggregated server-side)
     * and, when a token is set, PriceCharting. Everything else is a shortcut-only "open search" link
     * (CeX/Vinted/Tweakers, and eBay/Marktplaats links to complement the engine data).
     *
     * In demo mode (unused by default) eBay/PriceCharting fall back to mock data.
     */
    @Provides
    @Singleton
    fun provideSources(
        priceChartingApi: PriceChartingApi,
        engineApi: EngineApi,
        rewayApi: RewayApi,
        rewayThrottle: RewayThrottle,
        currencyConverter: CurrencyConverter,
        settingsRepository: SettingsRepository,
    ): List<@JvmSuppressWildcards MarketplaceSource> {
        val demo = BuildConfig.DEMO_MODE

        // Runtime PriceCharting token (Settings) with the build-time value as fallback.
        val tokenProvider: suspend () -> String = {
            settingsRepository.priceChartingToken.first().ifBlank { BuildConfig.PRICECHARTING_TOKEN }
        }
        val priceCharting: MarketplaceSource =
            if (demo) PriceChartingSource() else LivePriceChartingSource(priceChartingApi, tokenProvider, currencyConverter)

        // FlipIQ Engine — aggregated Marktplaats + eBay data when configured.
        val engine: MarketplaceSource? =
            if (BuildConfig.ENGINE_URL.isNotBlank()) {
                EngineSource(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY)
            } else if (demo) {
                EbaySoldSource() // demo fallback so there's some data
            } else {
                null
            }

        // Reway's two Shopify stores — a Dutch buy-in floor and retail ceiling (§6). No token gate;
        // toggled off in Settings if their theme ever breaks the endpoints. Buy-in is TRADE_IN so it
        // stays out of the resale median (§3); retail is ACTIVE and skips bulk Haul scans (§6).
        val rewayBuyIn: MarketplaceSource = RewaySource(
            api = rewayApi,
            throttle = rewayThrottle,
            id = RewaySource.BUY_IN_ID,
            displayName = "Reway (inkoop)",
            host = RewaySource.BUY_IN_HOST,
            listingType = ListingType.TRADE_IN,
            searchUrl = MarketplaceUrls::rewayBuyIn,
        )
        val rewayRetail: MarketplaceSource = RewaySource(
            api = rewayApi,
            throttle = rewayThrottle,
            id = RewaySource.RETAIL_ID,
            displayName = "Reway",
            host = RewaySource.RETAIL_HOST,
            listingType = ListingType.ACTIVE,
            searchUrl = MarketplaceUrls::rewayRetail,
            skipDuringHaul = true,
        )

        return listOfNotNull(
            engine,
            priceCharting,
            rewayBuyIn,
            rewayRetail,
            // Shortcut-only "open search" links.
            ShortcutOnlySource("ebay", "eBay", MarketplaceUrls::ebaySold),
            ShortcutOnlySource("marktplaats", "Marktplaats", MarketplaceUrls::marktplaats),
            // CeX's API is behind Cloudflare bot protection (403), so it's a search link for now.
            ShortcutOnlySource("cex", "CeX", MarketplaceUrls::cex),
            ShortcutOnlySource("vinted", "Vinted", MarketplaceUrls::vinted),
            ShortcutOnlySource("tweakers", "Tweakers", MarketplaceUrls::tweakers),
        )
    }

    /**
     * Barcode→title resolution. In release, prefer EAN-Search (token, strong EU/EAN coverage) and
     * fall back to keyless UPCitemdb. In demo, a no-op (mock sources already supply titles).
     */
    @Provides
    @Singleton
    fun provideBarcodeResolver(
        upcItemDbApi: UpcItemDbApi,
        eanSearchApi: EanSearchApi,
        openLibraryApi: OpenLibraryApi,
        settingsRepository: SettingsRepository,
    ): BarcodeResolver {
        if (BuildConfig.DEMO_MODE) return NoopBarcodeResolver()
        val eanToken: suspend () -> String = { settingsRepository.eanSearchToken.first() }
        return CompositeBarcodeResolver(
            listOf(
                OpenLibraryResolver(openLibraryApi),          // books (ISBN), keyless
                EanSearchResolver(eanSearchApi, eanToken),    // broad EU/EAN, needs token
                UpcItemDbResolver(upcItemDbApi),              // keyless fallback
            ),
        )
    }

    /** Snapshot → game title via the engine's vision model; no-op when the engine isn't configured. */
    @Provides
    @Singleton
    fun provideGameIdentifier(engineApi: EngineApi): GameIdentifier =
        if (BuildConfig.ENGINE_URL.isNotBlank()) {
            EngineGameIdentifier(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY)
        } else {
            NoopGameIdentifier()
        }

    /** Most-valuable-games-per-console browse data; no-op when the engine isn't configured. */
    @Provides
    @Singleton
    fun provideTopGamesService(engineApi: EngineApi): TopGamesService =
        if (BuildConfig.ENGINE_URL.isNotBlank()) {
            EngineTopGamesService(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY)
        } else {
            NoopTopGamesService()
        }

    /** Haul (many-items-in-one-photo) scanning; no-op when the engine isn't configured. */
    @Provides
    @Singleton
    fun provideHaulService(engineApi: EngineApi): HaulService =
        if (BuildConfig.ENGINE_URL.isNotBlank()) {
            EngineHaulService(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY)
        } else {
            NoopHaulService()
        }

    @Provides
    @Singleton
    fun providePriceRepository(
        sources: List<@JvmSuppressWildcards MarketplaceSource>,
        engine: FlipIQEngine,
        barcodeResolver: BarcodeResolver,
        settingsRepository: SettingsRepository,
    ): PriceRepository = PriceRepository(sources, engine, barcodeResolver, settingsRepository)
}
