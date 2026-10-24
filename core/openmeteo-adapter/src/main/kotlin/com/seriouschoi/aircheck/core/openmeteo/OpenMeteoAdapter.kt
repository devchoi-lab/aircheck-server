package com.seriouschoi.aircheck.core.openmeteo

import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

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
@Deprecated("현재 미사용. KmaAdapter 사용 중. 해외 좌표 지원 필요 시 재검토.")
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "openmeteo")
class OpenMeteoAdapter : WeatherPort {

    override fun getWeather(lat: Double, lng: Double): WeatherResponse? {
        throw UnsupportedOperationException(
            "OpenMeteoAdapter는 현재 미구현 상태입니다. weather.provider=kma 로 설정하세요."
        )
    }
}
