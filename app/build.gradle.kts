plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.3.0"
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.curiouskids.cashapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.curiouskids.cashapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        disable.add("UnusedMaterial3ScaffoldPaddingParameter")
        disable.add("IconMissingDensityFolder")
    }

    signingConfigs {
        create("release") {
            // Placeholder: Do not commit actual keystore or passwords!
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8000/api/v1/\"")
        }
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.skyjumpers.com/api/v1/\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            buildConfigField("String", "API_BASE_URL", "\"https://api.skyjumpers.com/api/v1/\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // material icon extended
    implementation(libs.androidx.compose.material.icons.extended)

    // kotlin x serialization
    implementation(libs.kotlinx.serialization.json)

    // usb serial - github-mik3y
    implementation(libs.usb.serial)

    // hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    // view-model lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // splashscreen
    implementation(libs.androidx.core.splashscreen)
    // navigation
    implementation(libs.androidx.navigation.compose)
    // datastore & security
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)
    implementation(libs.androidx.biometric)
    implementation(libs.play.services.auth.api.phone)
    implementation(libs.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.logging.interceptor)
    implementation(libs.zxing.core)
    implementation(libs.razorpay.checkout)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
