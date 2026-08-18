import java.io.File
import java.io.FileInputStream
import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.devtools.ksp)

    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    signingConfigs {
        create("release") {
            storeFile =
                file("/Users/dutchman/AndroidStudioProjects/android_apk_keystore/resumeiq.jks")
            storePassword = "ResumeIQ2026Password"
            keyPassword = "ResumeIQ2026Password"
            keyAlias = "ResumeIQAlias"
        }
    }
    namespace = "com.dutchman.resumeiq"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    val localProperties = Properties().apply {
        load(FileInputStream(File(rootProject.rootDir, "secrets.properties")))
    }

    defaultConfig {
        applicationId = "com.dutchman.resumeiq"
        versionCode = 10
        versionName = "1.0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES}"
        }
    }

    configurations.all {
        resolutionStrategy {
            force("androidx.concurrent:concurrent-futures:1.2.0")
            force("androidx.concurrent:concurrent-futures-ktx:1.2.0")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.bundles.androidx)
    implementation(libs.androidx.security.crypto)

    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.android.compiler)
//    ksp(libs.kotlinx.metadata)

    implementation(libs.bundles.androidx.hilt)
    implementation(libs.hilt.android)

    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Google Drive API
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.material)
    implementation(libs.compose.google.fonts)
    implementation(libs.bundles.navigation)

    implementation(libs.accompanist.permissions)

    implementation(libs.compose.navigation.core)
    ksp(libs.compose.navigation.ksp)

    implementation(libs.generative.ai)
    implementation(libs.collections.immutable)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.litert.lm)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.coil.compose)
    implementation(libs.compose.markdown)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.play.services.mlkit.document.scanner)

    implementation(libs.gson)

//    implementation(libs.vosk.android)


    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.auth)
    implementation(libs.googleid)

    // Networking
    implementation(platform(libs.okhttp.bom))
    implementation(libs.bundles.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.android)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.composeTest)

}