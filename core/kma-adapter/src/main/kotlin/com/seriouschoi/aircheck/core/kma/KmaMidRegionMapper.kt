package com.seriouschoi.aircheck.core.kma

import kotlin.math.pow

/**
 * 위경도 → 중기예보 예보구역코드(regId) 변환
 *
 * getMidLandFcst와 getMidTa는 격자 좌표(nx, ny) 대신 예보구역코드(regId)를 요구함.
 * 입력 좌표와 각 구역 대표 좌표 간 유클리드 거리로 최근접 구역을 반환.
 *
 * 대표 좌표 출처: 기상청 단기예보 격자_위경도(2610).xlsx 위도(초/100)·경도(초/100) 컬럼
 */
internal object KmaMidRegionMapper {

    /**
     * getMidLandFcst용 예보구역코드 반환 — 남한 10개 광역 구역.
     *
     * 범위 밖(북한·해외) 좌표 입력 시 서울·인천·경기(11B00000) 반환.
     */
    fun toLandRegId(lat: Double, lng: Double): String =
        LAND_REGIONS.minByOrNull { dist(it.lat, it.lng, lat, lng) }?.regId ?: FALLBACK_LAND_REG_ID

    /**
     * getMidTa용 예보구역코드 반환 — 남한 24개 대표 도시.
     *
     * 범위 밖(북한·해외) 좌표 입력 시 서울(11B10101) 반환.
     */
    fun toTaRegId(lat: Double, lng: Double): String =
        TA_REGIONS.minByOrNull { dist(it.lat, it.lng, lat, lng) }?.regId ?: FALLBACK_TA_REG_ID

    private fun dist(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double =
        (lat1 - lat2).pow(2) + (lng1 - lng2).pow(2)

    private const val FALLBACK_LAND_REG_ID = "11B00000"
    private const val FALLBACK_TA_REG_ID = "11B10101"

    private data class LandRegion(val regId: String, val lat: Double, val lng: Double)
    private data class TaRegion(val regId: String, val name: String, val lat: Double, val lng: Double)

    /**
     * getMidLandFcst 광역 구역 대표 좌표.
     * 각 구역의 대표 도시 좌표 기준.
     *
     * | regId    | 구역                 | 대표 도시 |
     * |----------|---------------------|---------|
     * | 11B00000 | 서울·인천·경기       | 서울     |
     * | 11D10000 | 강원영서             | 춘천     |
     * | 11D20000 | 강원영동             | 강릉     |
     * | 11C10000 | 충청북도             | 청주     |
     * | 11C20000 | 대전·세종·충청남도   | 대전     |
     * | 11F10000 | 전북자치도           | 전주     |
     * | 11F20000 | 광주·전라남도        | 광주     |
     * | 11H10000 | 대구·경상북도        | 대구     |
     * | 11H20000 | 부산·울산·경상남도   | 부산     |
     * | 11G00000 | 제주도               | 제주     |
     */
    private val LAND_REGIONS = listOf(
        LandRegion("11B00000", 37.5635694, 126.9800083),  // 서울·인천·경기
        LandRegion("11D10000", 37.8785417, 127.7323111),  // 강원영서 (춘천)
        LandRegion("11D20000", 37.7491361, 128.8784972),  // 강원영동 (강릉)
        LandRegion("11C10000", 36.5839972, 127.5117306),  // 충청북도 (청주)
        LandRegion("11C20000", 36.3471194, 127.3865667),  // 대전·세종·충청남도
        LandRegion("11F10000", 35.8091889, 127.1219194),  // 전북자치도 (전주)
        LandRegion("11F20000", 35.1711250, 126.9141222),  // 광주·전라남도 (광주)
        LandRegion("11H10000", 35.8685417, 128.6035528),  // 대구·경상북도
        LandRegion("11H20000", 35.1770194, 129.0769528),  // 부산·울산·경상남도
        LandRegion("11G00000", 33.4963111, 126.5332083),  // 제주도
    )

    /**
     * getMidTa 도시별 대표 좌표 (문서 기준 24개).
     * 출처: 기상청 중기예보 조회서비스 오픈API 활용가이드 첨부 코드 표.
     */
    private val TA_REGIONS = listOf(
        TaRegion("11B10101", "서울",   37.5635694, 126.9800083),
        TaRegion("11B20201", "인천",   37.4532333, 126.7073528),
        TaRegion("11B20601", "수원",   37.3010111, 127.0122222),
        TaRegion("11B20305", "파주",   37.7570833, 126.7819528),
        TaRegion("11D10301", "춘천",   37.8785417, 127.7323111),
        TaRegion("11D10401", "원주",   37.3390833, 127.9220556),
        TaRegion("11D20501", "강릉",   37.7491361, 128.8784972),
        TaRegion("11C20401", "대전",   36.3471194, 127.3865667),
        TaRegion("11C20101", "서산",   36.7820972, 126.4521639),
        TaRegion("11C20404", "세종",   36.4800121, 127.2890691),
        TaRegion("11C10301", "청주",   36.5839972, 127.5117306),
        TaRegion("11G00201", "제주",   33.4963111, 126.5332083),
        TaRegion("11G00401", "서귀포", 33.2540601, 126.5595500),
        TaRegion("11F20501", "광주",   35.1711250, 126.9141222),
        TaRegion("21F20801", "목포",   34.8087889, 126.3944194),
        TaRegion("11F20401", "여수",   34.7573111, 127.6643861),
        TaRegion("11F10201", "전주",   35.8091889, 127.1219194),
        TaRegion("21F10501", "군산",   35.9646417, 126.7388444),
        TaRegion("11H20201", "부산",   35.1770194, 129.0769528),
        TaRegion("11H20101", "울산",   35.5354083, 129.3136889),
        TaRegion("11H20301", "창원",   35.2540033, 128.6401544),
        TaRegion("11H10701", "대구",   35.8685417, 128.6035528),
        TaRegion("11H10501", "안동",   36.5654639, 128.7316222),
        TaRegion("11H10201", "포항",   36.0056861, 129.3616667),
    )
}
