plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "apps.visnkmr.batu"
    compileSdk = 34

    defaultConfig {
        applicationId = "visnkmr.apps.appstore"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.3"
    }

    buildTypes {
        release {
            // TEMP: disable code/resource shrinking to verify the Kotlin ICE isn't caused by R8/proguard
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    // Align Kotlin/Javac targets
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // Preview/tooling only for debug
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle + coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")


    // OkHttp (network for store list and chat models)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON (used throughout)
    implementation("org.json:json:20240303")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Remove Room and Kotlin serialization to reduce size (not used in current code paths)
    // Removed kapt and annotation forcing to slim classpath
}
