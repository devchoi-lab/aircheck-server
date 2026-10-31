import java.util.Properties

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

// local.properties → bootRun 환경변수
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    environment(localProps.entries.associate { (k, v) -> k.toString() to v.toString() })
}

dependencies {
    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:service"))
    implementation(project(":core:airkorea-adapter"))
    implementation(project(":core:openmeteo-adapter"))
    implementation(project(":core:bjd-matcher"))
    implementation(project(":core:kma-adapter"))
    implementation(project(":core:persistence-adapter"))
    implementation(project(":core:fcm-adapter"))
    implementation(project(":core:firebase-adapter"))
    
    // Feature modules
    implementation(project(":feature:weather"))
    implementation(project(":feature:admin"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Caffeine Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.seriouschoi.aircheck.AircheckServerApplicationKt")
    // 버전(allprojects의 version)이 바뀌어도 파일명이 안 바뀌게 고정 - deploy.yml/Dockerfile이 이 경로를 참조함
    archiveFileName.set("app.jar")
}
