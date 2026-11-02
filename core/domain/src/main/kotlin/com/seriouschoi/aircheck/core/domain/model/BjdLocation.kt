package com.seriouschoi.aircheck.core.domain.model

/**
 * 법정동(法定洞) 기반 위치 정보
 *
 * BjdPort.locate() 반환 타입.
 * 입력 위경도를 가장 가까운 법정동 대표 좌표로 정규화한 결과.
 *
 * @param doCode      법정동 도 단위 코드 (중기육상예보 지역 매핑용)
 * @param sigunguCode 법정동 시군구 단위 코드 (강원 영동/영서 구분용)
 * @param lat         법정동 대표 위도
 * @param lng         법정동 대표 경도
 */
data class BjdLocation(
    val doCode: Long,
    val sigunguCode: Long,
    val lat: Double,
    val lng: Double
)
