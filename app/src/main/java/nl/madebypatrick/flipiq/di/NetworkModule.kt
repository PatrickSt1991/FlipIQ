package nl.madebypatrick.flipiq.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import nl.madebypatrick.flipiq.BuildConfig
import nl.madebypatrick.flipiq.data.resolver.EanSearchApi
import nl.madebypatrick.flipiq.data.resolver.OpenLibraryApi
import nl.madebypatrick.flipiq.data.resolver.UpcItemDbApi
import nl.madebypatrick.flipiq.data.source.engine.EngineApi
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingApi
import nl.madebypatrick.flipiq.data.source.reway.RewayApi
import nl.madebypatrick.flipiq.data.source.reway.RewayThrottle
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val PRICECHARTING_TOKEN = "pricechartingToken"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        // The engine's /haul (Gemini lists titles + a PriceCharting scrape per item) and eBay-sold
        // lookups can legitimately take 20–40s; OkHttp's 10s default read timeout killed them
        // mid-flight, so haul "hung" then returned nothing. Give slow marketplace calls room.
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            },
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl("https://www.pricecharting.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providePriceChartingApi(retrofit: Retrofit): PriceChartingApi =
        retrofit.create(PriceChartingApi::class.java)

    @Provides
    @Singleton
    fun provideUpcItemDbApi(client: OkHttpClient, json: Json): UpcItemDbApi = Retrofit.Builder()
        .baseUrl("https://api.upcitemdb.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(UpcItemDbApi::class.java)

    @Provides
    @Singleton
    fun provideEanSearchApi(client: OkHttpClient, json: Json): EanSearchApi = Retrofit.Builder()
        .baseUrl("https://api.ean-search.org/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(EanSearchApi::class.java)

    @Provides
    @Singleton
    fun provideOpenLibraryApi(client: OkHttpClient, json: Json): OpenLibraryApi = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenLibraryApi::class.java)

    // Base is a placeholder — EngineApi.prices() uses @Url with the configured engine endpoint.
    @Provides
    @Singleton
    fun provideEngineApi(client: OkHttpClient, json: Json): EngineApi = Retrofit.Builder()
        .baseUrl("https://flipiq-engine.invalid/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(EngineApi::class.java)

    /**
     * Reway's two Shopify stores. Direct client calls (like PriceCharting), on a dedicated OkHttp
     * client whose interceptor (a) sets a repo-identifying User-Agent and (b) trips the shared
     * [RewayThrottle] circuit breaker on a 429, honouring `Retry-After` (§6). The base URL is a
     * placeholder — [RewayApi.suggest] uses `@Url` so one client serves both hosts.
     */
    @Provides
    @Singleton
    fun provideRewayApi(json: Json, throttle: RewayThrottle): RewayApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", RewayApi.REWAY_USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                val response = chain.proceed(request)
                if (response.code == 429) {
                    val retryAfter = response.header("Retry-After")?.trim()?.toLongOrNull()
                        ?: RewayThrottle.DEFAULT_BACKOFF_SECONDS
                    throttle.blockFor(retryAfter)
                }
                response
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()
        return Retrofit.Builder()
            .baseUrl("https://www.reway.nl/") // placeholder; @Url supplies the real per-call host
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RewayApi::class.java)
    }

    @Provides
    @Named(PRICECHARTING_TOKEN)
    fun providePriceChartingToken(): String = BuildConfig.PRICECHARTING_TOKEN
}
