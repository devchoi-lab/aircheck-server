plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":core:domain"))

    // Spring Web (Controller)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
}
