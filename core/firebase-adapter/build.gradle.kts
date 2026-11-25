plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Internal modules
    implementation(project(":core:domain"))
    
    // Spring
    implementation(libs.spring.context)
    implementation(libs.spring.web)
    
    // Jakarta
    implementation(libs.jakarta.servlet.api)
    implementation(libs.jakarta.annotation.api)
    
    // Firebase Admin (App Check 검증용)
    implementation(libs.firebase.admin)
    
    // JWT 검증
    implementation(libs.java.jwt)
    implementation(libs.jwks.rsa)
    
    // Logging
    implementation(libs.slf4j.api)
}
