plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") version "4.4.3"

}

android {
    namespace = "com.example.cardealer2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cardealer2"
        minSdk = 24
        targetSdk = 36
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
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    implementation(libs.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("io.coil-kt:coil-compose:2.7.0") // latest version
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    // Firebase
    implementation (platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation ("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-firestore")

    implementation ("com.google.firebase:firebase-storage")



    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.57.2")
    // Coroutine Support
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation ("com.google.zxing:core:3.5.3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.9.4")

    implementation("com.composables:icons-lucide-android:1.1.0")
    // Core library
    implementation("androidx.camera:camera-core:1.5.0")
// Camera2 implementation
    implementation("androidx.camera:camera-camera2:1.5.0")
// Lifecycle-aware components
    implementation("androidx.camera:camera-lifecycle:1.5.0")
// Preview, Video, and Image capture
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-video:1.5.0")
// Optional extensions (HDR, Night, Portrait, Beauty)
    implementation("androidx.camera:camera-extensions:1.5.0")
    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
    implementation("me.saket.telephoto:zoomable-image-coil:0.18.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.3")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")




}