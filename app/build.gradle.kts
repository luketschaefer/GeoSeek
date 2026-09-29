plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.geoseek"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.geoseek"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // TODO: Add when the features are built (not needed to compile yet):
    //  - CameraX (androidx.camera:camera-camera2, camera-lifecycle, camera-view) for the hunt camera
    //  - ML Kit Image Labeling (com.google.mlkit:image-labeling) for ObjectDetector
    //  - ARCore / SceneView (com.google.ar:core, io.github.sceneview:arsceneview) for AR later
}
