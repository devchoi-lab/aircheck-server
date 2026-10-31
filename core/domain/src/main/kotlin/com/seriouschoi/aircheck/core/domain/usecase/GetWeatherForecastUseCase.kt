package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import jakarta.inject.Inject
import jakarta.inject.Named

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetWeatherForecastUseCase @Inject constructor(
    private val bjdPort: BjdPort,
    private val weatherPort: WeatherPort
) {
    fun execute(lat: Double, lng: Double): WeatherResponse? {
        val location = bjdPort.locate(lat, lng)
        val normLat = location?.lat ?: lat
        val normLng = location?.lng ?: lng
        return weatherPort.getWeather(normLat, normLng)
    }
}
