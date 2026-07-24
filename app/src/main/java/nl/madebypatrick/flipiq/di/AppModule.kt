package nl.madebypatrick.flipiq.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import nl.madebypatrick.flipiq.BuildConfig
import nl.madebypatrick.flipiq.data.repository.PriceRepository
import nl.madebypatrick.flipiq.data.resolver.BarcodeResolver
import nl.madebypatrick.flipiq.data.resolver.NoopBarcodeResolver
import nl.madebypatrick.flipiq.data.resolver.UpcItemDbApi
import nl.madebypatrick.flipiq.data.resolver.UpcItemDbResolver
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ShortcutOnlySource
import nl.madebypatrick.flipiq.data.source.cex.CexApi
import nl.madebypatrick.flipiq.data.source.cex.CexSource
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
        currencyConverter: CurrencyConverter,
        settingsRepository: SettingsRepository,
    ): List<@JvmSuppressWildcards MarketplaceSource> {
        val demo = BuildConfig.DEMO_MODE

        // Runtime PriceCharting token (Settings) with the build-time value as fallback.
        val tokenProvider: suspend () -> String = {
            settingsRepository.priceChartingToken.first().ifBlank { BuildConfig.PRICECHARTING_TOKEN }
        }

        val ebay: MarketplaceSource =
            if (demo) EbaySoldSource() else ShortcutOnlySource("ebay", "eBay Sold", MarketplaceUrls::ebaySold)
        val priceCharting: MarketplaceSource =
            if (demo) PriceChartingSource() else LivePriceChartingSource(priceChartingApi, tokenProvider, currencyConverter)

        return listOf(
            ebay,
            priceCharting,
            CexSource(cexApi, currencyConverter),
            ShortcutOnlySource("vinted", "Vinted", MarketplaceUrls::vinted),
            ShortcutOnlySource("marktplaats", "Marktplaats", MarketplaceUrls::marktplaats),
        )
    }

    /** Real barcode→title lookup in release; a no-op in demo (mock sources already supply titles). */
    @Provides
    @Singleton
    fun provideBarcodeResolver(upcItemDbApi: UpcItemDbApi): BarcodeResolver =
        if (BuildConfig.DEMO_MODE) NoopBarcodeResolver() else UpcItemDbResolver(upcItemDbApi)

    @Provides
    @Singleton
    fun providePriceRepository(
        sources: List<@JvmSuppressWildcards MarketplaceSource>,
        engine: FlipIQEngine,
        barcodeResolver: BarcodeResolver,
    ): PriceRepository = PriceRepository(sources, engine, barcodeResolver)
}
