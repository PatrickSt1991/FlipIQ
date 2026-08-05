import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// API keys are read from local.properties (gitignored) or an env var — never committed.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(key: String, env: String): String =
    localProperties.getProperty(key) ?: System.getenv(env) ?: ""

android {
    namespace = "nl.madebypatrick.flipiq"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.madebypatrick.flipiq"
        minSdk = 24
        targetSdk = 35
        versionCode = 22
        versionName = "0.6.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Demo mode fills the engine with realistic mock data (eBay) for exploration.
        // Off by default (honest: only real sources feed the engine); the debug build turns it on.
        buildConfigField("boolean", "DEMO_MODE", "false")

        // Valoo Engine (Cloudflare Worker) — real Marktplaats + eBay data. Blank → those link out.
        // eBay creds now live in the engine Worker, not the app.
        buildConfigField("String", "ENGINE_URL", "\"${secret("engine.url", "ENGINE_URL")}\"")
        buildConfigField("String", "ENGINE_KEY", "\"${secret("engine.key", "ENGINE_KEY")}\"")
    }

    // Release signing is configured only when a keystore is supplied (via local.properties or CI
    // env/secrets); otherwise release builds stay unsigned so a keyless `assembleRelease` still runs.
    val releaseKeystore = secret("keystore.file", "KEYSTORE_FILE")
    signingConfigs {
        create("release") {
            if (releaseKeystore.isNotBlank()) {
                storeFile = file(releaseKeystore)
                storePassword = secret("keystore.password", "KEYSTORE_PASSWORD")
                keyAlias = secret("key.alias", "KEY_ALIAS")
                keyPassword = secret("key.password", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Mock data disabled — debug builds use the real sources too.
            buildConfigField("boolean", "DEMO_MODE", "false")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (releaseKeystore.isNotBlank()) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking (wired now; real sources plugged in later)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Settings persistence
    implementation(libs.androidx.datastore.preferences)

    // Background price-alert checks
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Images
    implementation(libs.coil.compose)

    // Scanning
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)
    implementation(libs.accompanist.permissions)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
