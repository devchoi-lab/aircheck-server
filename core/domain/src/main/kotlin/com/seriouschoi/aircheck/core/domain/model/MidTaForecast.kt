package com.seriouschoi.aircheck.core.domain.model

import java.time.LocalDate

/**
 * 중기기온예보 (3~10일, getMidTa API 응답)
 *
 * lat/lng → taRegId 매핑 후 조회. KmaWeatherResponse에 포함되어 반환.
 */
data class MidTaForecast(
    val days: List<MidTaForecastDay>
)

/**
 * @param date       예보 날짜 (KST 기준). 발표일 + 3~10일
 * @param minTemp    최저기온 (℃). 값이 없으면 null
 * @param maxTemp    최고기온 (℃). 값이 없으면 null
 */
data class MidTaForecastDay(
    val date: LocalDate,
    val minTemp: Int?,
    val maxTemp: Int?
)
