package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import jakarta.inject.Inject
import jakarta.inject.Named

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetCombinedWeatherUseCase @Inject constructor(
    private val bjdPort: BjdPort,
    private val kmaWeatherPort: KmaWeatherPort,
    private val airQualityPort: AirQualityPort
) {
    fun execute(lat: Double, lng: Double): CombinedWeatherResult {
        val location = bjdPort.locate(lat, lng)
        val kmaWeather = location?.let { kmaWeatherPort.getWeather(it).getOrNull() }
        return CombinedWeatherResult(
            kmaWeather = kmaWeather,
            airQuality = airQualityPort.getAirQuality(location?.lat ?: lat, location?.lng ?: lng)
        )
    }
}
