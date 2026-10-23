package com.seriouschoi.aircheck.core.openmeteo

import com.seriouschoi.aircheck.core.domain.model.CurrentWeather
import com.seriouschoi.aircheck.core.domain.model.HourlyForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ── Open-Meteo API 응답 모델 ───────────────────────────────────────────────

@Serializable
data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
    val hourly: OpenMeteoHourly? = null
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("apparent_temperature") val feelsLike: Double = 0.0,
    @SerialName("precipitation") val precipitation: Double = 0.0,
    @SerialName("weathercode") val weatherCode: Int = 0,
    @SerialName("cloudcover") val cloudCover: Int = 0,
    @SerialName("is_day") val isDay: Int = 1
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double> = emptyList(),
    @SerialName("apparent_temperature") val feelsLike: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int> = emptyList(),
    val snowfall: List<Double> = emptyList(),
    @SerialName("weathercode") val weatherCode: List<Int> = emptyList()
)

// ── Adapter 구현 ───────────────────────────────────────────────────────────

/**
 * Open-Meteo API Adapter
 *
 * 현재 미사용 — 서비스는 KmaAdapter(weather.provider=kma) 기준으로 운영.
 * 해외 좌표 지원 등 필요 시 weather.provider=openmeteo 로 전환.
 *
 * 활성화: weather.provider=openmeteo
 * ※ matchIfMissing 제거됨 — 환경변수 미설정 시 자동 활성화되지 않음.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "openmeteo")
class OpenMeteoAdapter(
    private val apiUsagePort: com.seriouschoi.aircheck.core.service.port.ApiUsagePort?
) : WeatherPort {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val API_TYPE = "OPEN_METEO"
    }

    override fun getWeather(lat: Double, lng: Double): WeatherResponse? {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lng" +
                "&current=temperature_2m,apparent_temperature,precipitation,weathercode,cloudcover,is_day" +
                "&hourly=temperature_2m,apparent_temperature,precipitation_probability,snowfall,weathercode" +
                "&forecast_hours=48" +
                "&timezone=auto"
        
        val startTime = System.currentTimeMillis()
        
        return try {
            val response = get(url)
            val parsed = json.decodeFromString<OpenMeteoResponse>(response)
            
            val current = parsed.current ?: return null
            val hourly = parsed.hourly ?: return null
            
            // API 호출 성공 기록
            val responseTime = System.currentTimeMillis() - startTime
            apiUsagePort?.recordSuccess(API_TYPE, responseTime)
            
            WeatherResponse(
                current = CurrentWeather(
                    temperature = current.temperature,
                    feelsLike = current.feelsLike,
                    precipitation = current.precipitation,
                    weatherCode = current.weatherCode,
                    weatherCondition = WeatherCondition.fromWeatherCode(current.weatherCode),
                    cloudCover = current.cloudCover,
                    isDay = current.isDay == 1
                ),
                hourlyForecast = hourly.time.mapIndexed { index, time ->
                    val dateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    // Open-Meteo는 UTC 반환 (기본 설정)
                    val utcInstant = dateTime.toInstant(ZoneOffset.UTC)
                    HourlyForecast(
                        time = utcInstant,
                        hour = dateTime.hour,  // UTC 시간
                        temperature = hourly.temperature.getOrElse(index) { 0.0 },
                        feelsLike = hourly.feelsLike.getOrElse(index) { 0.0 },
                        precipitationProbability = hourly.precipitationProbability.getOrElse(index) { 0 },
                        snowfall = hourly.snowfall.getOrElse(index) { 0.0 },
                        weatherCode = hourly.weatherCode.getOrElse(index) { 0 },
                        weatherCondition = WeatherCondition.fromWeatherCode(
                            hourly.weatherCode.getOrElse(index) { 0 }
                        )
                    )
                }
            )
        } catch (e: Exception) {
            log.error("Open-Meteo API 호출 실패: ${e.message}")
            apiUsagePort?.recordFailure(API_TYPE, e.message)
            null
        }
    }
    
    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
