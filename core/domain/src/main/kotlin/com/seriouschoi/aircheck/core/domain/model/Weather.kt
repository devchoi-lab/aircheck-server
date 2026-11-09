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
    @Schema(description = "날씨 상태. 초단기실황은 하늘상태(SKY)를 주지 않아 강수형태(PTY)로 비/눈일 때만 RAIN/SNOW. 강수 없음이거나 값이 없으면 null", nullable = true)
    val weatherCondition: WeatherCondition?,
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
    @Schema(description = "날씨. 하늘상태(SKY)나 강수형태(PTY) 값이 없으면 null", nullable = true)
    val weatherCondition: WeatherCondition?
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
    @Schema(description = "오전 날씨. 하늘상태(SKY)나 강수형태(PTY) 값이 없으면 null", nullable = true)
    val amWeatherCondition: WeatherCondition?,
    @Schema(description = "오후 날씨. 하늘상태(SKY)나 강수형태(PTY) 값이 없으면 null", nullable = true)
    val pmWeatherCondition: WeatherCondition?
)


enum class WeatherCondition(val label: String, val emoji: String) {
    CLEAR("맑음", "☀️"),
    PARTLY_CLOUDY("구름 조금", "⛅"),
    CLOUDY("흐림", "☁️"),
    FOG("안개", "🌫️"),
    DRIZZLE("이슬비", "🌧️"),
    RAIN("비", "🌧️"),
    HEAVY_RAIN("폭우", "⛈️"),
    SNOW("눈", "🌨️"),
    HEAVY_SNOW("폭설", "❄️"),
    THUNDERSTORM("뇌우", "⛈️");

    companion object {
        fun fromWeatherCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            61, 63, 80, 81 -> RAIN
            65, 82 -> HEAVY_RAIN
            71, 73, 85 -> SNOW
            75, 86 -> HEAVY_SNOW
            95, 96, 99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}

data class CombinedWeatherResult(
    val kmaWeather: KmaWeatherResponse?,
    val airQuality: AirQualityResponse?,
    val errors: List<DataError> = emptyList()
)

/**
 * CombinedWeatherResult 전용 에러 정보.
 * errorCode: 클라이언트가 UI 분기에 사용.
 * errorId: 서버 로그에서 grep으로 추적 가능한 UUID.
 */
data class DataError(
    val errorCode: DataErrorCode,
    val errorId: String
)

/**
 * CombinedWeatherResult 전용 에러코드.
 */
enum class DataErrorCode {
    API_UNAVAILABLE,    // 외부 API 호출 실패
    LOCATION_NOT_FOUND  // 좌표 → BJD 변환 실패 (한국 외 지역 등)
}
