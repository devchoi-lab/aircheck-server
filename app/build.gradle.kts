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

// Spring Boot BOM의 Testcontainers 버전(1.20.x)을 덮어쓴다. Docker 29는 이 버전의 Docker API(1.32)를 거부한다. (#138)
// mariadb/junit-jupiter가 전이로 끌어오는 코어(testcontainers, jdbc, database-commons)까지 맞추려고 필요하다.
// 지우면 이 코어들만 BOM 버전으로 내려간다. 확인: ./gradlew :app:dependencyInsight --dependency org.testcontainers --configuration integrationTestRuntimeClasspath
extra["testcontainers.version"] = libs.versions.testcontainers.get()

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
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    
    // Kotlin
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    
    // Caffeine Cache
    implementation(libs.caffeine)
    
    // OpenAPI / Swagger UI
    implementation(libs.springdoc.openapi.webmvc.ui)
    
    // Database
    runtimeOnly(libs.mariadb.java.client)

    // Test
    testImplementation(libs.spring.boot.starter.test)

    // 통합 테스트(src/integrationTest): 진짜 MariaDB 컨테이너 + 외부 API(기상청/에어코리아) 가짜 서버
    "integrationTestImplementation"(libs.spring.boot.starter.test)
    "integrationTestImplementation"(libs.testcontainers.mariadb)
    "integrationTestImplementation"(libs.testcontainers.junit.jupiter)
    "integrationTestImplementation"(libs.wiremock.standalone)
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
