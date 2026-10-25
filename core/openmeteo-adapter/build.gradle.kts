plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core:domain"))

    // Spring
    implementation("org.springframework:spring-context:6.2.4")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
