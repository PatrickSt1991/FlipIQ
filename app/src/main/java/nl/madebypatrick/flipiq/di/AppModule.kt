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
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingApi
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingSource as LivePriceChartingSource
import nl.madebypatrick.flipiq.domain.engine.EngineConfig
import nl.madebypatrick.flipiq.domain.engine.FlipIQEngine
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEngine(): FlipIQEngine = FlipIQEngine(EngineConfig.DEFAULT)

    /**
     * The active set of marketplace sources. PriceCharting is the live one when a token is
     * configured, otherwise its mock stands in; the rest are still mock-backed. Adding another real
     * source later is just a swap in this list — nothing downstream changes.
     */
    @Provides
    @Singleton
    fun provideSources(
        priceChartingApi: PriceChartingApi,
        @Named(NetworkModule.PRICECHARTING_TOKEN) priceChartingToken: String,
    ): List<@JvmSuppressWildcards MarketplaceSource> {
        val priceCharting: MarketplaceSource =
            if (priceChartingToken.isNotBlank()) {
                LivePriceChartingSource(priceChartingApi, priceChartingToken)
            } else {
                PriceChartingSource()
            }
        return listOf(
            EbaySoldSource(),
            priceCharting,
            CexSource(),
            VintedSource(),
            MarktplaatsSource(),
        )
    }

    @Provides
    @Singleton
    fun providePriceRepository(
        sources: List<@JvmSuppressWildcards MarketplaceSource>,
        engine: FlipIQEngine,
    ): PriceRepository = PriceRepository(sources, engine)
}
