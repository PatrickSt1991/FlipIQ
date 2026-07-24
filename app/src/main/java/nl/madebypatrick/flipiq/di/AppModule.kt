package nl.madebypatrick.flipiq.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.madebypatrick.flipiq.data.repository.PriceRepository
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.mock.CexSource
import nl.madebypatrick.flipiq.data.source.mock.EbaySoldSource
import nl.madebypatrick.flipiq.data.source.mock.MarktplaatsSource
import nl.madebypatrick.flipiq.data.source.mock.PriceChartingSource
import nl.madebypatrick.flipiq.data.source.mock.VintedSource
import nl.madebypatrick.flipiq.domain.engine.EngineConfig
import nl.madebypatrick.flipiq.domain.engine.FlipIQEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEngine(): FlipIQEngine = FlipIQEngine(EngineConfig.DEFAULT)

    /**
     * The active set of marketplace sources. Today these are the mock implementations; wiring in a
     * real one later means adding it to this list — nothing else changes.
     */
    @Provides
    @Singleton
    fun provideSources(): List<@JvmSuppressWildcards MarketplaceSource> = listOf(
        EbaySoldSource(),
        PriceChartingSource(),
        CexSource(),
        VintedSource(),
        MarktplaatsSource(),
    )

    @Provides
    @Singleton
    fun providePriceRepository(
        sources: List<@JvmSuppressWildcards MarketplaceSource>,
        engine: FlipIQEngine,
    ): PriceRepository = PriceRepository(sources, engine)
}
