import java.util.Properties

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
        versionCode = 13
        versionName = "2.2"

        // Where lessons are published: the `content` branch of this repository (see docs/content-guide.md).
        buildConfigField("String", "CONTENT_URL", "\"${providers.gradleProperty("youtop.contentUrl").getOrElse("")}\"")
        // Google Drive folder "YouTop books": where lessons were published up to 1.6, still read
        // when GitHub cannot be reached and the phone has no GitHub content yet. (shared as "anyone with the link").
        // Can be changed inside the app (Settings) without rebuilding.
        buildConfigField("String", "DEFAULT_DRIVE_FOLDER", "\"${providers.gradleProperty("youtop.driveFolder").getOrElse("")}\"")
        // Optional Drive API key. Without it the app reads the public folder page.
        buildConfigField("String", "DRIVE_API_KEY", "\"${providers.gradleProperty("youtop.driveApiKey").getOrElse("")}\"")
    }

    // The release key is not in the repository. tools/setup-signing.sh restores it into keystore/
    // from the private file in Google Drive. Without it the build still works, signed with the
    // machine's debug key, but such an APK cannot be installed over a properly signed one.
    val signingFile = rootProject.file("keystore/signing.properties")
    val releaseKey = rootProject.file("keystore/youtop-release.jks")
    val hasReleaseKey = signingFile.exists() && releaseKey.exists()
    if (!hasReleaseKey) logger.warn("YouTop: no release key in keystore/ - signing with the debug key (see CLAUDE.md).")

    signingConfigs {
        if (hasReleaseKey) {
            val props = Properties().apply { signingFile.inputStream().use { load(it) } }
            create("release") {
                storeFile = releaseKey
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        val signing = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
        debug {
            signingConfig = signing
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signing
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
