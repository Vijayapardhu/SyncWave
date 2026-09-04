plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.syncwave.core.ui"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.11" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-P=plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.23"
    }
}

dependencies {
    api(platform("androidx.compose:compose-bom:2024.05.00"))
    api("androidx.compose.material3:material3")
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-tooling-preview")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.animation:animation")
    api("androidx.activity:activity-compose:1.9.0")
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    api("com.google.zxing:core:3.5.3")
}
