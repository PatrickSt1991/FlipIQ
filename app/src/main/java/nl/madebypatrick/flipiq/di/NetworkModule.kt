package nl.madebypatrick.flipiq.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import nl.madebypatrick.flipiq.BuildConfig
import nl.madebypatrick.flipiq.data.source.cex.CexApi
import nl.madebypatrick.flipiq.data.source.pricecharting.PriceChartingApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    fun provideCexApi(client: OkHttpClient, json: Json): CexApi = Retrofit.Builder()
        .baseUrl("https://wss2.cex.uk.webuy.io/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CexApi::class.java)

    @Provides
    @Named(PRICECHARTING_TOKEN)
    fun providePriceChartingToken(): String = BuildConfig.PRICECHARTING_TOKEN
}
