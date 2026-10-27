plugins {
    kotlin("jvm")
}

/**
 * CSV 재생성 task
 *
 * 실행: ./gradlew :core:kma-region:generateMidRegionCsv
 *
 * 입력 (worknote/):
 *   - 기상청28_중기예보 조회서비스_오픈API활용가이드_251212/중기예보_중기기온예보구역코드_2025.12.xlsx
 *   - 기상청41_단기예보 조회서비스_오픈API활용가이드_2610/기상청41_단기예보 조회서비스_오픈API활용가이드_격자_위경도(2610).xlsx
 *
 * 출력:
 *   - src/main/resources/mid-region-latlon.csv
 *
 * 생성 후 결과를 검수하고 커밋할 것.
 */
tasks.register<Exec>("generateMidRegionCsv") {
    group = "kma-region"
    description = "중기예보 예보구역코드 + 위경도 매핑 CSV 생성"
    commandLine("python3", "scripts/generate_mid_region_csv.py")
    workingDir = projectDir
}
