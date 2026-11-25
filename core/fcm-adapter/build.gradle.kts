plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":core:domain"))
    
    // Spring
    implementation(libs.spring.context)
    
    // Jakarta (for @PostConstruct)
    implementation(libs.jakarta.annotation.api)
    
    // Firebase Admin (FCM)
    implementation(libs.firebase.admin)
    
    // Logging
    implementation(libs.slf4j.api)
}
