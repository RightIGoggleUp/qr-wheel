plugins {
    id("com.android.application")
    kotlin("android")
}
android {
    namespace = "com.anotherpatkreation.qrwheel"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.anotherpatkreation.qrwheel"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "0.1.0-test12"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
}
