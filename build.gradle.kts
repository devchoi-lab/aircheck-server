plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.serialization") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    id("org.springframework.boot") version "3.4.4" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

allprojects {
    group = "com.seriouschoi"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configurations.matching { it.name.startsWith("detekt") }.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("1.9.23")
            }
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
    }
    
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.4")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
