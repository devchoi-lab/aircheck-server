package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import jakarta.inject.Inject
import jakarta.inject.Named

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetCombinedWeatherUseCase @Inject constructor(
    private val weatherPort: WeatherPort,
    private val airQualityPort: AirQualityPort
) {
    fun execute(lat: Double, lng: Double): CombinedWeatherResult =
        CombinedWeatherResult(
            weather = weatherPort.getWeather(lat, lng),
            airQuality = airQualityPort.getAirQuality(lat, lng)
        )
}
