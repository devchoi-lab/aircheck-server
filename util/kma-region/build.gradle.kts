plugins {
    kotlin("jvm")
}

/**
 * 중기예보 위경도 매핑 CSV 재생성 task
 *
 * 실행: ./gradlew :util:kma-region:generateMidRegionCsv
 *
 * 입력 (scripts/):
 *   - mid-region-codes.csv : 예보구역코드·구역명·특성 (xlsx 업데이트 시 extract_source_csvs.py 재실행)
 *   - grid-latlon.csv      : 시도·시군구·읍면동·위도·경도
 *
 * 출력:
 *   - src/main/resources/mid-region-latlon.csv
 *
 * 생성 후 결과를 검수하고 커밋할 것.
 * xlsx 원본이 바뀐 경우: python3 scripts/extract_source_csvs.py 먼저 실행.
 */
tasks.register<Exec>("generateMidRegionCsv") {
    group = "kma-region"
    description = "중기예보 예보구역코드 + 위경도 매핑 CSV 생성"
    commandLine("python3", "scripts/generate_mid_region_csv.py")
    workingDir = projectDir
}
