plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core:domain"))

    // Spring
    implementation(libs.spring.boot.starter.web)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.slf4j.api)

    // Test
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}
