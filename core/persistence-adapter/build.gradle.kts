plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":core:domain"))

    // Spring Data JPA
    implementation(libs.spring.boot.starter.data.jpa)
    
    // Database
    runtimeOnly(libs.mariadb.java.client)
}
