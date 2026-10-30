import java.util.Properties

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":core:domain"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}

/**
 * 법정동(法定洞) 위경도 매핑 CSV 재생성 task
 *
 * 실행: ./gradlew :core:bjd-matcher:generateBjdCsv
 *
 * 사전 조건: local.properties에 kakao.api.key 설정 필요
 *   kakao.api.key=<Kakao REST API 키>
 *
 * 입력 (scripts/):
 *   - 국토교통부_전국 법정동_20260630.csv : 법정동 코드표 원본
 *
 * 출력:
 *   - src/main/resources/bjd-lat-lng.csv
 *
 * 생성 후 결과를 검수하고 커밋할 것.
 */
tasks.register<Exec>("generateBjdCsv") {
    group = "bjd-matcher"
    description = "법정동 위경도 매핑 CSV 생성"

    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())
    val kakaoApiKey = localProps.getProperty("kakao.api.key")
        ?: error("local.properties에 kakao.api.key가 없음")

    commandLine("python3", "scripts/generate_bjd_csv.py", kakaoApiKey)
    workingDir = projectDir
}
