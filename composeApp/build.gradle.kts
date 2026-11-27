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
                implementation(compose.ui)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.3")
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
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        applicationId = "com.redfox.gram"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.2-M3E"
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
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "RedFoxGram"
            packageVersion = "1.0.0"
        }
    }
}

val generateRedFoxSecrets by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/redfox/kotlin")
    outputs.dir(outDir)
    doLast {
        val apiId = System.getenv("TELEGRAM_API_ID")?.toIntOrNull() ?: 0
        val apiHash = System.getenv("TELEGRAM_API_HASH") ?: ""
        val file = outDir.get().file("com/redfox/gram/config/RedFoxSecrets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText("package com.redfox.gram.config\nobject RedFoxSecrets { const val API_ID = $apiId; const val API_HASH = \"$apiHash\" }\n")
    }
}
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateRedFoxSecrets)
