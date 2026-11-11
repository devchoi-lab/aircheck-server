package com.seriouschoi.aircheck.core.domain.exception

/**
 * 조회는 성공했지만 쓸 수 있는 대기질 데이터가 없을 때.
 * (주변 측정소가 없거나, 주변 측정소 모두 PM10/PM2.5 값이 없는 경우)
 *
 * API 호출 자체가 실패한 경우와 구분하기 위한 예외다.
 */
class AirQualityNotFoundException(
    val lat: Double,
    val lng: Double,
    reason: String
) : DataNotFoundException("대기질 데이터가 없습니다: $reason (lat=$lat, lng=$lng)")
