plugins {
    kotlin("jvm")
}

dependencies {
    // OpenAPI Schema annotations
    compileOnly(libs.swagger.annotations)

    // DI 표준 어노테이션 (Jakarta EE / JSR-330) — Spring 비의존
    compileOnly(libs.jakarta.inject.api)

    // 로깅 유틸리티 (errorWithId 확장함수 포함)
    implementation(project(":util:logging"))

    // Test
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}
