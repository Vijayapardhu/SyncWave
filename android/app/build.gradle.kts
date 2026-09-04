plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.syncwave"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.syncwave"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.11" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-P=plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.23"
    }

    buildTypes {
        debug {
            buildConfigField("String", "SYNCWAVE_BASE_URL", "\"https://sync-6n67glif7-vijayapardhus-projects.vercel.app\"")
        }
        release {
            buildConfigField("String", "SYNCWAVE_BASE_URL", "\"https://sync-6n67glif7-vijayapardhus-projects.vercel.app\"")
        }
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:signaling"))
    implementation(project(":core:webrtc"))
    implementation(project(":core:media"))
    implementation(project(":feature:home"))
    implementation(project(":feature:room"))
    implementation(project(":feature:audio"))
    implementation(project(":feature:scan"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
}
