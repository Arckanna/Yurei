plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.valerie.yurei"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.valerie.yurei"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { compose = true }

    // Utilise la version du compiler depuis le catalog
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }

    // Aligne Java/Kotlin sur 17 pour éviter le mismatch javac 1.8 / Kotlin 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.lifecycle.get()}")
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":audio"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.runtime)

    // Navigation Compose (via catalog)
    implementation(libs.navigation.compose)
    implementation(libs.navigation.runtime.ktx)

    // ViewModel pour Compose
    implementation(libs.lifecycle.viewmodel.compose)

    // Coroutines Android (boucle de jeu / scopes)
    implementation(libs.kotlinx.coroutines.android)

    // Tooling debug
    debugImplementation(libs.compose.ui.tooling)
}

