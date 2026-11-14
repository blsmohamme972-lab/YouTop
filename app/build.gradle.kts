plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.youtop"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.youtop"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-prototype"

        // Google Drive folder "YouTop books" (shared as "anyone with the link").
        // Can be changed inside the app (Settings) without rebuilding.
        buildConfigField("String", "DEFAULT_DRIVE_FOLDER", "\"${providers.gradleProperty("youtop.driveFolder").getOrElse("")}\"")
        // Optional Drive API key. Without it the app reads the public folder page.
        buildConfigField("String", "DRIVE_API_KEY", "\"${providers.gradleProperty("youtop.driveApiKey").getOrElse("")}\"")
    }

    signingConfigs {
        // Prototype key kept in the repo so every build (local or CI) can update the previous one.
        create("prototype") {
            storeFile = rootProject.file("keystore/youtop-prototype.jks")
            storePassword = "youtop-prototype"
            keyAlias = "youtop"
            keyPassword = "youtop-prototype"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("prototype")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("prototype")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Save pictures from ScreensTest instead of comparing against stored ones.
                it.systemProperty("roborazzi.test.record", "true")
                // Lets `-Drobolectric.dependency.repo.url=...` through to the test JVM.
                System.getProperties().forEach { (k, v) -> if (k.toString().startsWith("robolectric.")) it.systemProperty(k.toString(), v) }
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.datastore)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.roborazzi)
}
