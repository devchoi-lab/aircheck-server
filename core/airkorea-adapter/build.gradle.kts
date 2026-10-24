plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:service"))
    
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
