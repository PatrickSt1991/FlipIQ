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
     * **Real price data** comes from the FlipIQ Engine (Marktplaats + eBay-sold, aggregated
     * server-side). The rest are shortcut-only "open search" links (eBay/Marktplaats) that complement
     * the engine data with a one-tap way to browse the live listings.
     *
     * In demo mode (unused by default) the engine falls back to mock eBay-sold data.
     */
    @Provides
    @Singleton
    fun provideSources(
        engineApi: EngineApi,
        currencyConverter: CurrencyConverter,
        settingsRepository: SettingsRepository,
    ): List<@JvmSuppressWildcards MarketplaceSource> {
        val demo = BuildConfig.DEMO_MODE

        // FlipIQ Engine — aggregated Marktplaats + eBay(-sold) data when configured.
        val engine: MarketplaceSource? =
            if (BuildConfig.ENGINE_URL.isNotBlank()) {
                EngineSource(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY) {
                    settingsRepository.ebayLocation.first().param
                }
            } else if (demo) {
                EbaySoldSource() // demo fallback so there's some data
            } else {
                null
            }

        // The engine's aggregated market data first, then the two "open in browser" links.
        return listOfNotNull(
            engine,
            ShortcutOnlySource("ebay", "eBay", MarketplaceUrls::ebaySold),
            ShortcutOnlySource("marktplaats", "Marktplaats", MarketplaceUrls::marktplaats),
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
    fun provideTopGamesService(
        engineApi: EngineApi,
        settingsRepository: SettingsRepository,
    ): TopGamesService =
        if (BuildConfig.ENGINE_URL.isNotBlank()) {
            EngineTopGamesService(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY) {
                settingsRepository.ebayLocation.first().param
            }
        } else {
            NoopTopGamesService()
        }

    /** Haul (many-items-in-one-photo) scanning; no-op when the engine isn't configured. */
    @Provides
    @Singleton
    fun provideHaulService(
        engineApi: EngineApi,
        settingsRepository: SettingsRepository,
    ): HaulService =
        if (BuildConfig.ENGINE_URL.isNotBlank()) {
            EngineHaulService(engineApi, BuildConfig.ENGINE_URL, BuildConfig.ENGINE_KEY) {
                settingsRepository.ebayLocation.first().param
            }
        } else {
            NoopHaulService()
        }

    @Provides
    @Singleton
    fun providePriceRepository(
        sources: List<@JvmSuppressWildcards MarketplaceSource>,
        engine: FlipIQEngine,
        settingsRepository: SettingsRepository,
    ): PriceRepository = PriceRepository(sources, engine, settingsRepository)
}
