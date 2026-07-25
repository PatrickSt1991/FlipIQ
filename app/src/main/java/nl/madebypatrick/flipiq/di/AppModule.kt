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
import nl.madebypatrick.flipiq.data.source.cex.CexApi
import nl.madebypatrick.flipiq.data.source.cex.CexSource
import nl.madebypatrick.flipiq.data.source.ebay.EbayApi
import nl.madebypatrick.flipiq.data.source.ebay.EbayAuthenticator
import nl.madebypatrick.flipiq.data.source.ebay.EbaySource
import nl.madebypatrick.flipiq.data.source.engine.EngineApi
import nl.madebypatrick.flipiq.data.source.engine.EngineSource
import nl.madebypatrick.flipiq.data.source.mock.EbaySoldSource
import nl.madebypatrick.flipiq.data.source.mock.PriceChartingSource
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingApi
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingSource as LivePriceChartingSource
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
     * Live price data: **CeX** (keyless) and **PriceCharting** (needs a token, from Settings or the
     * build). **Vinted** and **Marktplaats** are shortcut-only (no API, no scraping) — they link to
     * the site's search but never feed prices to the engine.
     *
     * In **demo mode** (debug builds) eBay and PriceCharting are backed by realistic mock data so the
     * app is explorable; a release build only feeds the engine real numbers and links out for the rest.
     */
    @Provides
    @Singleton
    fun provideSources(
        priceChartingApi: PriceChartingApi,
        cexApi: CexApi,
        ebayApi: EbayApi,
        engineApi: EngineApi,
        currencyConverter: CurrencyConverter,
        settingsRepository: SettingsRepository,
    ): List<@JvmSuppressWildcards MarketplaceSource> {
        val demo = BuildConfig.DEMO_MODE

        // Runtime PriceCharting token (Settings) with the build-time value as fallback.
        val tokenProvider: suspend () -> String = {
            settingsRepository.priceChartingToken.first().ifBlank { BuildConfig.PRICECHARTING_TOKEN }
        }

        // eBay: mock in demo; live Browse API when the token proxy is configured; otherwise a link.
        val ebayAuth = EbayAuthenticator(ebayApi, BuildConfig.EBAY_PROXY_URL, BuildConfig.EBAY_PROXY_KEY)
        val ebay: MarketplaceSource = when {
            demo -> EbaySoldSource()
            ebayAuth.isConfigured -> EbaySource(ebayApi, ebayAuth)
            else -> ShortcutOnlySource("ebay", "eBay Sold", MarketplaceUrls::ebaySold)
        }
        val priceCharting: MarketplaceSource =
            if (demo) PriceChartingSource() else LivePriceChartingSource(priceChartingApi, tokenProvider, currencyConverter)

        // Marktplaats: real data via the FlipIQ Engine when configured, else a search link.
        val marktplaats: MarketplaceSource =
            if (BuildConfig.ENGINE_URL.isNotBlank()) {
                EngineSource(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY)
            } else {
                ShortcutOnlySource("marktplaats", "Marktplaats", MarketplaceUrls::marktplaats)
            }

        return listOf(
            ebay,
            priceCharting,
            CexSource(cexApi, currencyConverter),
            ShortcutOnlySource("vinted", "Vinted", MarketplaceUrls::vinted),
            marktplaats,
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

    @Provides
    @Singleton
    fun providePriceRepository(
        sources: List<@JvmSuppressWildcards MarketplaceSource>,
        engine: FlipIQEngine,
        barcodeResolver: BarcodeResolver,
    ): PriceRepository = PriceRepository(sources, engine, barcodeResolver)
}
