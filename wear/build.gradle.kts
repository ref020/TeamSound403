plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.alexmercerind.audire.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alexmercerind.audire"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}


dependencies {
    // --- Core Compose & Wear OS Dependencies ---
    implementation(platform("androidx.compose:compose-bom:2024.09.00")) // BOM helps manage versions
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-material:1.3.1") // Provides Material components for Wear
    implementation("androidx.wear.compose:compose-foundation:1.3.1") // Provides foundational components

    // --- Activity & Lifecycle ---
    implementation("androidx.activity:activity-compose:1.9.0")

    // --- Navigation for Wear OS ---
    implementation("androidx.wear.compose:compose-navigation:1.3.1")

    // --- Icons ---
    implementation("androidx.compose.material:material-icons-extended") // Provides the Icons library

    // --- Communication with the phone app ---
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    // --- Splash Screen ---
    implementation("androidx.core:core-splashscreen:1.0.1")

    // --- For debug builds ---
    debugImplementation("androidx.compose.ui:ui-tooling")
}

