package com.seriouschoi.aircheck.core.domain.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate


/**
 * 현재 날씨 (초단기실황).
 * 기상청 응답에 해당 항목이 없거나 읽을 수 없으면 기본값으로 채우지 않고 null.
 */
@Schema(description = "현재 날씨")
data class CurrentWeather(
    @Schema(description = "기온 (℃). 값이 없으면 null", example = "20.7", nullable = true)
    val temperature: Double?,
    @Schema(description = "체감온도 (℃). 서버 계산값 — 기온 ≤10℃+풍속 ≥1.3m/s: Wind Chill, 기온 ≥27℃: Heat Index. 계산에 필요한 값이 없으면 null", example = "18.5", nullable = true)
    val feelsLike: Double?,
    @Schema(description = "1시간 강수량 (mm). 값이 없으면 null", example = "0.0", nullable = true)
    val precipitation: Double?,
    @Schema(description = "습도 (%). 값이 없으면 null", example = "49", nullable = true)
    val humidity: Int?,
    @Schema(description = "풍속 (m/s). 값이 없으면 null", example = "1.8", nullable = true)
    val windSpeed: Double?,
    @Schema(description = "풍향 (deg, 0~360). 16방위 변환: (deg + 11.25) / 22.5 → 정수 인덱스. 값이 없으면 null", example = "158", nullable = true)
    val windDirection: Int?,
    @Schema(description = "강수형태 (기상청 PTY). 초단기실황은 하늘상태(SKY)를 주지 않는다. 값이 없으면 null", nullable = true)
    val precipitationType: PrecipitationType?,
    @Schema(description = "낮 여부. KST 기준 6~18시이면 true", example = "true")
    val isDay: Boolean
)

data class HourlyForecast(
    val time: Instant,
    @Schema(description = "기온 (℃). 값이 없으면 null", nullable = true)
    val temperature: Double?,
    @Schema(description = "체감온도 (℃). 계산에 필요한 값이 없으면 null", nullable = true)
    val feelsLike: Double?,
    @Schema(description = "강수확률 (%). 값이 없으면 null", nullable = true)
    val precipitationProbability: Int?,
    @Schema(description = "적설. 기상청 SNO는 범주 문자열이라 숫자로 읽을 수 없으면 null", nullable = true)
    val snowfall: Double?,
    @Schema(description = "하늘상태 (기상청 SKY). 값이 없으면 null", nullable = true)
    val sky: SkyCondition?,
    @Schema(description = "강수형태 (기상청 PTY). 값이 없으면 null", nullable = true)
    val precipitationType: PrecipitationType?
)

/**
 * 일별 예보 (단기예보 3일)
 */
data class DailyForecast(
    @Schema(description = "예보 날짜 (KST 기준, YYYY-MM-DD). 최저/최고기온과 오전/오후 구분도 KST 하루 기준", example = "2026-03-20")
    val date: LocalDate,
    @Schema(description = "최저기온 (℃). 기상청 응답에 값이 없으면 null", nullable = true)
    val minTemp: Double?,
    @Schema(description = "최고기온 (℃). 기상청 응답에 값이 없으면 null", nullable = true)
    val maxTemp: Double?,
    @Schema(description = "오전(12시 이전) 강수확률 최댓값 (%). 오전 예보 값이 없으면 null", nullable = true)
    val amPrecipProb: Int?,
    @Schema(description = "오후(12시 이후) 강수확률 최댓값 (%). 오후 예보 값이 없으면 null", nullable = true)
    val pmPrecipProb: Int?,
    @Schema(description = "오전 하늘상태 (기상청 SKY). 오전 예보 값이 없으면 null", nullable = true)
    val amSky: SkyCondition?,
    @Schema(description = "오후 하늘상태 (기상청 SKY). 오후 예보 값이 없으면 null", nullable = true)
    val pmSky: SkyCondition?,
    @Schema(description = "오전 강수형태 (기상청 PTY). 오전 예보 값이 없으면 null", nullable = true)
    val amPrecipitationType: PrecipitationType?,
    @Schema(description = "오후 강수형태 (기상청 PTY). 오후 예보 값이 없으면 null", nullable = true)
    val pmPrecipitationType: PrecipitationType?
)

/**
 * 초단기실황 + 단기예보 (현재날씨·시간별·일별)
 */
data class ShortForecast(
    val current: CurrentWeather,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>
)
