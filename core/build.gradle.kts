plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.valerie.yurei.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Compose pour les types géométriques (Offset, Size)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    // Corroutines pour les boucles et tâches asynchrones
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${libs.versions.coroutines.get()}")
}
