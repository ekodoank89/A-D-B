plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ===== Konfigurasi dari GitHub Secrets (env) =====
val mapsApiKey: String = (System.getenv("MAPS_API_KEY") ?: "").trim()
val keystoreFilePath: String = System.getenv("KEYSTORE_FILE") ?: "adb-release.jks"
val keystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")
val keyAlias: String? = System.getenv("KEY_ALIAS")
val keyPassword: String? = System.getenv("KEY_PASSWORD")

android {
    namespace = "awali.dengan.bismillah"
    compileSdk = 35 // Android 15

    defaultConfig {
        applicationId = "awali.dengan.bismillah"
        minSdk = 30    // Android 11
        targetSdk = 35 // Android 15
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        if (keystorePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                storeFile = file(keystoreFilePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Google Maps Compose SDK
    implementation("com.google.maps.android:maps-compose:6.1.2")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

}
