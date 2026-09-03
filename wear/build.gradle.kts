plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.apollox10.apollodeck.wear"
    // 35, not 34 like the other modules — androidx.wear.tiles:tiles:1.6.0
    // (and its transitive protolayout-material3 dependency) requires it.
    compileSdk = 35

    defaultConfig {
        // Must exactly match the phone app's applicationId (com.apollox10.apollodeck)
        // — Play Services' Wear Data Layer (DataClient/MessageClient) only
        // routes between a phone app and watch app it can verify are "the
        // same app": matching package name AND matching signing
        // certificate. A different id here (it used to be
        // "com.apollox10.apollodeck.wear") makes every DataClient/
        // MessageClient call between them silently fail — confirmed via
        // logcat ("Failed to deliver message to AppKey...") and a
        // getDataItems() that only ever came back empty, both against real
        // paired hardware, until this was changed. namespace (below) is
        // unaffected — it only names the generated R/BuildConfig package.
        applicationId = "com.apollox10.apollodeck"
        // Wear OS 3+ only — required for Wear Compose / modern Tiles.
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    // Tiles render icons as rasterized bitmaps baked into inline protolayout
    // image resources (there's no live Compose tree for a Tile to draw
    // into) — same Material Icons dictionary the phone app uses for
    // dashboard/widget icons, see wear/.../icons/IconMapping.kt.
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    debugImplementation(libs.androidx.ui.tooling)
}
