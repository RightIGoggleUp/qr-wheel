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
        versionCode = 9
        versionName = "0.1.0-test9"
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
}
