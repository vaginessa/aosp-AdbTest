@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "dev.rohitverma882.adbtest"
    compileSdk = project.properties["compileSdk"].toString().toInt()

    defaultConfig {
        applicationId = "dev.rohitverma882.adbtest"
        minSdk = project.properties["minSdk"].toString().toInt()
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = true
    }
}

dependencies {
    implementation(project(":adbutils"))

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}