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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        // The engine's /haul (Gemini lists titles + a marketplace lookup per item) and eBay-sold
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
}
