import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.3")
                implementation("androidx.core:core-ktx:1.15.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.redfox.gram"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.redfox.gram"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-Expressive"
        buildConfigField("int", "TELEGRAM_API_ID", System.getenv("TELEGRAM_API_ID") ?: "0")
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${System.getenv("TELEGRAM_API_HASH") ?: ""}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.redfox.gram.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.AppImage, TargetFormat.Deb, TargetFormat.Exe, TargetFormat.Msi)
            packageName = "RedFoxGram"
            packageVersion = "0.1.0"
            description = "RedFoxGram — Material 3 Expressive Cross-Platform Telegram Client"
            vendor = "RedFox Dev"
        }
    }
}
