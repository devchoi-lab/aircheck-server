package com.seriouschoi.aircheck.core.domain.model

/**
 * 통합 날씨 결과. 모든 항목은 선택이며, 실패한 항목은 null로 두고 [errors]에 이유를 담는다.
 * 법정동을 찾지 못한 경우는 부분 결과가 아니라 전체 실패([com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException])다.
 */
data class CombinedWeatherResult(
    val kmaWeather: KmaWeather,
    val airQuality: AirQuality?,
    val errors: List<DataError> = emptyList()
)
