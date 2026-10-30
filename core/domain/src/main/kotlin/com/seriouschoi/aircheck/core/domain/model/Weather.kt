package com.seriouschoi.aircheck.core.domain.model

import com.seriouschoi.aircheck.core.domain.serializer.InstantSerializer
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 날씨 응답 (앱으로 전달)
 */
@Serializable
data class WeatherResponse(
    val current: CurrentWeather,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast> = emptyList(),
    val midTermForecast: List<MidTermForecast> = emptyList()
)

@Schema(description = "현재 날씨")
@Serializable
data class CurrentWeather(
    @Schema(description = "기온 (℃)", example = "20.7")
    val temperature: Double,
    @Schema(description = "체감온도 (℃). 서버 계산값 — 기온 ≤10℃+풍속 ≥1.3m/s: Wind Chill, 기온 ≥27℃: Heat Index", example = "18.5")
    val feelsLike: Double,
    @Schema(description = "1시간 강수량 (mm). 강수 없으면 0.0", example = "0.0")
    val precipitation: Double,
    @Schema(description = "습도 (%)", example = "49")
    val humidity: Int,
    @Schema(description = "풍속 (m/s)", example = "1.8")
    val windSpeed: Double,
    @Schema(description = "풍향 (deg, 0~360). 16방위 변환: (deg + 11.25) / 22.5 → 정수 인덱스", example = "158")
    val windDirection: Int,
    @Schema(description = "날씨 상태. 초단기실황 특성상 PTY=0이면 항상 CLEAR (SKY 미제공)")
    val weatherCondition: WeatherCondition,
    @Schema(description = "낮 여부. KST 기준 6~18시이면 true", example = "true")
    val isDay: Boolean
)

@Serializable
data class HourlyForecast(
    @Serializable(with = InstantSerializer::class)
    val time: Instant,
    val hour: Int,  // KST 기준 시간 (표시용)
    val temperature: Double,
    val feelsLike: Double,
    val precipitationProbability: Int,
    val snowfall: Double,
    val weatherCode: Int,
    val weatherCondition: WeatherCondition
)

/**
 * 일별 예보 (단기예보 3일)
 */
@Serializable
data class DailyForecast(
    val date: String,              // YYYY-MM-DD
    val dayOfWeek: String,         // 월, 화, 수...
    val minTemp: Double,           // 최저기온
    val maxTemp: Double,           // 최고기온
    val amPrecipProb: Int,         // 오전 강수확률
    val pmPrecipProb: Int,         // 오후 강수확률
    val amWeatherCondition: WeatherCondition,
    val pmWeatherCondition: WeatherCondition
)

/**
 * 중기예보 (3~10일)
 */
@Serializable
data class MidTermForecast(
    val date: String,              // YYYY-MM-DD
    val dayOfWeek: String,         // 월, 화, 수...
    val minTemp: Int,              // 최저기온
    val maxTemp: Int,              // 최고기온
    val amPrecipProb: Int,         // 오전 강수확률
    val pmPrecipProb: Int,         // 오후 강수확률
    val weatherDescription: String // 날씨 설명 (맑음, 구름많음 등)
)

@Serializable
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
    val weather: WeatherResponse?,
    val airQuality: AirQualityResponse?
)
