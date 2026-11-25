plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":core:domain"))

    // Spring Web (Controller)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    
    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    
    // OpenAPI / Swagger
    implementation(libs.springdoc.openapi.webmvc.ui)
}
