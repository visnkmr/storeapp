plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "apps.visnkmr.batu"
    compileSdk = 34

    defaultConfig {
        applicationId = "apps.visnkmr.batu"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    // Align Kotlin/Javac targets to avoid kapt mismatch
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
    androidTestImplementation(composeBom)

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

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Security crypto for encrypted prefs (keep minimal Tink set via R8)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // OkHttp (no logging dependency)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON
    implementation("org.json:json:20240303")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Serialization for simple JSON mapping (we'll still keep org.json where already used)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Download manager helpers (no extra dep required; use android.app.DownloadManager)
    // PackageInstaller is part of framework.

    // Ensure Kotlin compiler classpath has JetBrains annotations available (needed by kapt/IR)
    // Keep javax.annotation exclusion only
    configurations.all {
        exclude(group = "javax.annotation", module = "javax.annotation-api")
        resolutionStrategy {
            // Align all org.jetbrains:annotations usages to 23.0.0 to satisfy constraints
            force("org.jetbrains:annotations:23.0.0")
        }
    }

    // Keep annotations available at compile; match forced version
    compileOnly("org.jetbrains:annotations:23.0.0")
    kapt("org.jetbrains:annotations:23.0.0")
}
