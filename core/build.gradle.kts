plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Librairie standard Kotlin
    implementation(kotlin("stdlib"))

    // Corroutines pour les boucles et tâches asynchrones
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
