plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.airecipeapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.airecipeapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // === APK ABI splits (use only if you produce APKs; skip if you publish AAB) ===
    // Kotlin DSL syntax for splits / abi
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true          // R8
            isShrinkResources = true        // resource shrinking
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release") // add if using Gradle signing
        }
        getByName("debug") {
            // debug-specific options (optional)
        }
    }

    compileOptions {
        // Align with modern AGP / Gradle recommendations — make sure your JDK/Gradle use Java 17.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // Match Java target
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Uncomment / set if you pin a specific Compose compiler version
        // kotlinCompilerExtensionVersion = "1.xx.x"
    }

    packaging {
        resources {
            // keep your META-INF exclusions
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

        // Modern JNI packaging option (Kotlin DSL)
        jniLibs {
            // prefer non-legacy packaging when supported by AGP (improves packaging behavior)
            // Set to `true` if you explicitly want legacy (compressed) packaging for native libs.
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Text Recognition (Unbundled - Play Services)
    implementation(libs.play.services.mlkit.text.recognition)

    // Kotlinx Coroutines Play Services (for ML Kit await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // ExifInterface
    implementation(libs.androidx.exifinterface)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
