plugins {
    kotlin("jvm")
}

dependencies {
    // OpenAPI Schema annotations
    compileOnly("io.swagger.core.v3:swagger-annotations:2.2.19")

    // DI 표준 어노테이션 (Jakarta EE / JSR-330) — Spring 비의존
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")

    // 로깅 유틸리티 (errorWithId 확장함수 포함)
    implementation(project(":util:logging"))
}
