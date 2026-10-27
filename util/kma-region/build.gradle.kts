import java.util.Properties

plugins {
    kotlin("jvm")
}

/**
 * 중기예보 위경도 매핑 CSV 재생성 task
 *
 * 실행: ./gradlew :util:kma-region:generateMidRegionCsv
 *
 * 사전 조건: local.properties에 kakao.api.key 설정 필요
 *   kakao.api.key=<Kakao REST API 키>
 *
 * 입력 (scripts/):
 *   - mid-region-codes.csv : 예보구역코드·구역명·특성·쿼리
 *
 * 출력:
 *   - src/main/resources/mid-region-latlon.csv
 *
 * 생성 후 결과를 검수하고 커밋할 것.
 */
tasks.register<Exec>("generateMidRegionCsv") {
    group = "kma-region"
    description = "중기예보 예보구역코드 + 위경도 매핑 CSV 생성"

    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())
    val kakaoApiKey = localProps.getProperty("kakao.api.key")
        ?: error("local.properties에 kakao.api.key가 없음")

    commandLine("python3", "scripts/generate_mid_region_csv.py", kakaoApiKey)
    workingDir = projectDir
}
