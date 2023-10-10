plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.metalichesky.intentintercept"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.metalichesky.intentintercept"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidCore)
    implementation(libs.androidAppCompat)
    implementation(libs.androidMaterial)
    implementation(libs.androidPreference)
}