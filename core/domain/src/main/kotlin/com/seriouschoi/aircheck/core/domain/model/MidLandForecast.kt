package com.seriouschoi.aircheck.core.domain.model

/**
 * 중기육상예보 (3~10일, getMidLandFcst API 응답)
 *
 * doCode → landRegId 매핑 후 조회. UseCase에서 MidTaForecast와 조합해 MidTermForecast 생성.
 */
data class MidLandForecast(
    val days: List<MidLandForecastDay>
)

/**
 * @param dayOffset  기준일 기준 일수 (3~10)
 * @param amPrecipProb  오전 강수확률 (%). 8~10일은 단일값을 am/pm 공통 사용
 * @param pmPrecipProb  오후 강수확률 (%)
 * @param amWeather     오전 날씨 설명
 * @param pmWeather     오후 날씨 설명
 */
data class MidLandForecastDay(
    val dayOffset: Int,
    val amPrecipProb: Int,
    val pmPrecipProb: Int,
    val amWeather: String,
    val pmWeather: String
)
