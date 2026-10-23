package com.seriouschoi.aircheck.core.domain.model

import com.seriouschoi.aircheck.core.domain.serializer.InstantSerializer
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

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val precipitation: Double,
    val weatherCondition: WeatherCondition,
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
