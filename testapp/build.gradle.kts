plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Helper: ambil secret dari environment (sama seperti modul :app)
fun envSecret(name: String): String? =
    System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

val envStore = envSecret("KEYSTORE_PASSWORD")
val envAlias = envSecret("KEY_ALIAS")
val envKeyPass = envSecret("KEY_PASSWORD")

android {
    namespace = "awali.dengan.bismillah.loctest"
    compileSdk = 35

    defaultConfig {
        applicationId = "awali.dengan.bismillah.loctest"
        minSdk = 30    // Android 11
        targetSdk = 35 // Android 15
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (envStore != null && envAlias != null && envKeyPass != null) {
            create("release") {
                // Keystore di-decode workflow ke app/adb-release.jks — dipakai bersama
                storeFile = rootProject.file("app/adb-release.jks")
                storePassword = envStore
                keyAlias = envAlias
                keyPassword = envKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
