android {
    ...
    signingConfigs {
        create("release") {
            // Membaca lokasi keystore yang di-generate di langkah 4
            storeFile = file("release.jks")
            
            // Membaca kredensial dari Environment Variables yang dikirim di langkah 5
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Menghubungkan konfigurasi penandatanganan ke release build
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
