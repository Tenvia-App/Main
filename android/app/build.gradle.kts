import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing key lives OUTSIDE the repo (C:\Users\aries\tenvia-signing) and is never committed.
val signingProps = Properties().apply {
    val f = file("C:/Users/aries/tenvia-signing/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "app.tenvia"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.tenvia"
        // Android 8.0: keeps Tenvia usable on older and cheaper phones.
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "0.6.0-ai-test"
    }

    signingConfigs {
        if (signingProps.isNotEmpty()) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (signingProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // AI TEST BUILD: the llama.cpp server is bundled so the app can start the on-phone AI itself
    // (a server started over USB dies when the cable is unplugged). The binaries are not in git:
    // they live in android/llama-bin/jniLibs (pulled from the phone). They are extracted on install
    // so the server can be run as a program.
    sourceSets["main"].jniLibs.srcDir("../llama-bin/jniLibs")
    // Tester build: the small model file (~1 GB) rides inside the app, uncompressed, from
    // android/llama-bin/assets/models (not in git). AiServer copies it out on first start.
    sourceSets["main"].assets.srcDir("../llama-bin/assets")
    androidResources {
        noCompress += "gguf"
    }
    packaging {
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")

    testImplementation("junit:junit:4.13.2")
    // Real org.json for unit tests (Android's copy is only a stub off-device), so tests can read guides.json.
    testImplementation("org.json:json:20250517")
}
