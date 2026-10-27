package com.seriouschoi.aircheck.core.kma.region

import kotlin.math.pow

/**
 * 위경도 → 중기예보 예보구역코드(regId) 변환
 *
 * getMidLandFcst와 getMidTa는 격자 좌표(nx, ny) 대신 예보구역코드(regId)를 요구함.
 * 입력 좌표와 각 구역 대표 좌표 간 유클리드 거리로 최근접 구역을 반환.
 *
 * 좌표 데이터 출처: mid-region-latlon.csv (기상청 공식 문서 기반 생성)
 *   - 예보구역코드·구역명: 중기예보_중기기온예보구역코드_2025.12.xlsx
 *   - 위경도: 기상청41_단기예보 조회서비스_오픈API활용가이드_격자_위경도(2610).xlsx
 *   - CSV 재생성: ./gradlew :core:kma-region:generateMidRegionCsv
 */
object KmaMidRegionMapper {

    /**
     * getMidLandFcst용 예보구역코드 반환 — 남한 광역 구역 (A타입).
     *
     * 범위 밖(북한·해외) 좌표 입력 시 서울·인천·경기(11B00000) 반환.
     */
    fun toLandRegId(lat: Double, lng: Double): String =
        landRegions.minByOrNull { dist(it.lat, it.lng, lat, lng) }?.regId ?: FALLBACK_LAND_REG_ID

    /**
     * getMidTa용 예보구역코드 반환 — 도시별 기온 구역 (C타입).
     *
     * 범위 밖(북한·해외) 좌표 입력 시 서울(11B10101) 반환.
     */
    fun toTaRegId(lat: Double, lng: Double): String =
        taRegions.minByOrNull { dist(it.lat, it.lng, lat, lng) }?.regId ?: FALLBACK_TA_REG_ID

    private fun dist(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double =
        (lat1 - lat2).pow(2) + (lng1 - lng2).pow(2)

    private const val FALLBACK_LAND_REG_ID = "11B00000"
    private const val FALLBACK_TA_REG_ID = "11B10101"
    private const val CSV_RESOURCE = "/mid-region-latlon.csv"

    private data class Region(val regId: String, val lat: Double, val lng: Double)

    private val landRegions: List<Region> by lazy { loadRegions("A") }
    private val taRegions: List<Region> by lazy { loadRegions("C") }

    private fun loadRegions(type: String): List<Region> {
        val stream = KmaMidRegionMapper::class.java.getResourceAsStream(CSV_RESOURCE)
            ?: error("리소스를 찾을 수 없음: $CSV_RESOURCE")
        return stream.bufferedReader().useLines { lines ->
            lines.drop(1)  // 헤더 스킵
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size < 5) return@mapNotNull null
                    val regId = parts[0]
                    val lat = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                    val lng = parts[3].toDoubleOrNull() ?: return@mapNotNull null
                    val t = parts[4]
                    if (t == type) Region(regId, lat, lng) else null
                }
                .toList()
        }
    }
}
