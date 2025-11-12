import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")

}

android {
    namespace = "com.alexmercerind.audire"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alexmercerind.audire"
        minSdk = 21
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        buildConfigField(
            "String",
            "GENIUS_ACCESS_TOKEN",
            "\"lotCNW5w74n4u71jWHC4CDel93El7Br1EJhn43bgut9HJTr1hifaP1RRNe6_zMIB\""
        )
        buildConfigField(
            "String",
            "YOUTUBE_API_KEY",
            "\"AIzaSyBR2VhkUNrR5BIjNbrzMeBKQT3RvD_0E08\""
        )

        manifestPlaceholders += mapOf(
            "redirectSchemeName" to "audire",
            "redirectHostName"  to "callback"
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    val navVersion = "2.9.3"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    val roomVersion = "2.7.2"
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    implementation("com.github.f4b6a3:uuid-creator:6.1.1")

    implementation("io.coil-kt:coil:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("org.jsoup:jsoup:1.17.2")

    implementation("com.spotify.android:auth:2.1.0")
    implementation("org.json:json:20230227")

    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")


    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
