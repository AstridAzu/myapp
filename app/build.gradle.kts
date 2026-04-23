plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

import java.util.Properties

fun Project.resolveConfigValue(key: String, localProps: Properties): String? {
    val gradleValue = findProperty(key)?.toString()?.takeIf { it.isNotBlank() }
    if (gradleValue != null) return gradleValue

    val localValue = localProps.getProperty(key)?.takeIf { it.isNotBlank() }
    if (localValue != null) return localValue

    return System.getenv(key)?.takeIf { it.isNotBlank() }
}

val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.myapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        val imageApiBaseUrl = project.resolveConfigValue("IMAGE_API_BASE_URL", localProps)
            ?: "https://ratita-gym--worker.azucenapolo6.workers.dev"
        val imageApiToken = project.resolveConfigValue("IMAGE_API_TOKEN", localProps) ?: ""
        val syncApiBaseUrl = project.resolveConfigValue("SYNC_API_BASE_URL", localProps)
            ?: imageApiBaseUrl
        val syncApiToken = project.resolveConfigValue("SYNC_API_TOKEN", localProps)
            ?: imageApiToken
        val syncRemoteIdStrategy = project.resolveConfigValue("SYNC_REMOTE_ID_STRATEGY", localProps)
            ?: "STRICT"
        buildConfigField("String", "IMAGE_API_BASE_URL", "\"$imageApiBaseUrl\"")
        buildConfigField("String", "IMAGE_API_TOKEN", "\"$imageApiToken\"")
        buildConfigField("String", "SYNC_API_BASE_URL", "\"$syncApiBaseUrl\"")
        buildConfigField("String", "SYNC_API_TOKEN", "\"$syncApiToken\"")
        buildConfigField("String", "SYNC_REMOTE_ID_STRATEGY", "\"$syncRemoteIdStrategy\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
dependencies {
    // Import the Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Core & AppCompat
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.coil.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Sync infra
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.12")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
}

// Comentario para forzar la re-sincronización de Gradle y limpiar cachés corruptas de KSP.