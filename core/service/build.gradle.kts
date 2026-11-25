plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":core:domain"))
    
    // Spring (interface만 사용)
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    
    // Logging
    implementation(libs.slf4j.api)
    
    // Test
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
