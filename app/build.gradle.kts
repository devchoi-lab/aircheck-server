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

// 통합 테스트는 단위 테스트(src/test)와 소스셋을 분리한다.
// - ./gradlew test             : 단위 테스트 (Docker 불필요)
// - ./gradlew :app:integrationTest : 통합 테스트 (Docker 필요, 느림)
val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[integrationTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.implementation.get())
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.runtimeOnly.get())

// Docker 29(최소 API 1.40)는 Spring Boot 3.4.4가 관리하는 Testcontainers 1.20.x(API 1.32)를 거부한다.
extra["testcontainers.version"] = "1.21.4"

dependencies {
    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:service"))
    implementation(project(":core:airkorea-adapter"))
    implementation(project(":core:bjd-matcher"))
    implementation(project(":core:kma-adapter"))
    implementation(project(":core:persistence-adapter"))
    implementation(project(":core:fcm-adapter"))
    implementation(project(":core:firebase-adapter"))
    
    // Feature modules
    implementation(project(":feature:weather"))
    implementation(project(":feature:push"))
    implementation(project(":feature:app-version"))
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
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    
    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // 통합 테스트(src/integrationTest): 진짜 MariaDB 컨테이너 + 외부 API(기상청/에어코리아) 가짜 서버
    "integrationTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "integrationTestImplementation"("org.testcontainers:mariadb")
    "integrationTestImplementation"("org.testcontainers:junit-jupiter")
    "integrationTestImplementation"("org.wiremock:wiremock-standalone:3.9.2")
}

tasks.register<Test>("integrationTest") {
    description = "통합 테스트 (Docker 필요)"
    group = "verification"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.seriouschoi.aircheck.AircheckServerApplicationKt")
    // 버전(allprojects의 version)이 바뀌어도 파일명이 안 바뀌게 고정 - deploy.yml/Dockerfile이 이 경로를 참조함
    archiveFileName.set("app.jar")
}
