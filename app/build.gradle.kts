import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    // KSP for Room
    alias(libs.plugins.google.devtools.ksp)
    // Firebase Services
    alias(libs.plugins.google.gms.google.services)
}

base {
    archivesName.set("Real Weather - ${libs.versions.app.versionName.get()}")
}

android {
    namespace = "com.aqi.weather"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aqi.weather"
        minSdk = 26
        targetSdk = 36
        versionName = libs.versions.app.versionName.get()
        versionCode = libs.versions.app.versionCode.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties().apply { load(rootProject.file("local.properties").inputStream()) }
        val webId = localProps.getProperty("WEB_CLIENT_ID") ?: error("WEB_CLIENT_ID not found in local.properties")
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webId\"")
        val weatherApiKey = localProps.getProperty("WEATHER_API_KEY") ?: error("WEATHER_API_KEY not found in local.properties")
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
        val locationApiKey = localProps.getProperty("LOCATION_API_KEY") ?: error("LOCATION_API_KEY not found in local.properties")
        buildConfigField("String", "LOCATION_API_KEY", "\"$locationApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // For UI
    implementation (libs.sdp.android)
    implementation (libs.ssp.android)
    implementation (libs.glide)
    implementation (libs.lottie)
    implementation (libs.otpview)
    implementation (libs.ccp)
    implementation (libs.material.calendar.view)

    // Location
    implementation(libs.play.services.location)

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    // Auth
    implementation(libs.play.services.auth) // Google
    implementation(libs.facebook.android.sdk) // Facebook

    // Fragment Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)
    // To use Kotlin Symbol Processing (KSP)
    ksp(libs.androidx.room.compiler)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // LiveData
    implementation(libs.androidx.lifecycle.livedata.ktx)
    // Fragment KTX for viewModels()
    implementation (libs.androidx.fragment.ktx)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit2.converter.gson)
    // OkHttp for logging
    implementation(libs.logging.interceptor)

    // The core LiteRT (TFLite) dependency from Play Services
    implementation(libs.play.services.tflite.java)
    // Optional: Support Library for easier model handling
    implementation(libs.play.services.tflite.support)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
}