package com.seriouschoi.aircheck.core.domain.exception

/**
 * 입력 위경도로 법정동을 찾지 못했을 때.
 *
 * 법정동으로 정규화하지 못한 좌표로는 외부 API를 호출하지 않는다 (캐시 키가 좌표 기준이라 호출 건수가 샌다).
 */
class LocationNotFoundException(
    val lat: Double,
    val lng: Double
) : DataNotFoundException("법정동을 찾을 수 없습니다 (lat=$lat, lng=$lng)")
