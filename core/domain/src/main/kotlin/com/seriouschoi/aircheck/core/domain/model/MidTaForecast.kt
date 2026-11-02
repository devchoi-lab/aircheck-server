package com.seriouschoi.aircheck.core.domain.model

/**
 * 중기기온예보 (3~10일, getMidTa API 응답)
 *
 * lat/lng → taRegId 매핑 후 조회. UseCase에서 MidLandForecast와 조합해 MidTermForecast 생성.
 */
data class MidTaForecast(
    val days: List<MidTaForecastDay>
)

/**
 * @param dayOffset  기준일 기준 일수 (3~10)
 * @param minTemp    최저기온 (℃)
 * @param maxTemp    최고기온 (℃)
 */
data class MidTaForecastDay(
    val dayOffset: Int,
    val minTemp: Int,
    val maxTemp: Int
)
