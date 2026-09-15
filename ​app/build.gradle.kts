plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "module.aya.doank"
    compileSdk = 35

    defaultConfig {
        applicationId = "module.aya.doank"
        minSdk = 30 // Android 11 (API 30)
        targetSdk = 35 // Android 15 (API 35)
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] = System.getenv("MAPS_API_KEY") ?: ""
    }

    signingConfigs {
        create("release") {
            // Akan diambil dari GitHub Secrets via Github Actions
            val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
            if (!keystoreBase64.isNullOrEmpty()) {
                val keystoreFile = file("${layout.buildDirectory.get()}/release.jks")
                keystoreFile.parentFile.mkdirs()
                keystoreFile.writeBytes(java.util.Base64.getDecoder().decode(keystoreBase64))
                
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Google Maps & Location
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // LSPosed API (Modern Hook Support API 102)
    compileOnly(libs.lsposed.api)
}
