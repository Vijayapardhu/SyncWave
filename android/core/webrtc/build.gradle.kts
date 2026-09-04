plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.syncwave.core.webrtc"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:media"))
    implementation(project(":core:network"))
    implementation(project(":core:signaling"))

    implementation("androidx.annotation:annotation:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Google's libwebrtc for Android (Chromium M104, mirrored on JitPack).
    api("com.github.webrtc-sdk:android:104.5112.10")
}
