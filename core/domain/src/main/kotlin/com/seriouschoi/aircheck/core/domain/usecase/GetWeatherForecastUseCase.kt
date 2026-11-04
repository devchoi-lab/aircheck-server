package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.KmaWeatherResponse
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import jakarta.inject.Inject
import jakarta.inject.Named

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetWeatherForecastUseCase @Inject constructor(
    private val bjdPort: BjdPort,
    private val kmaWeatherPort: KmaWeatherPort
) {
    fun execute(lat: Double, lng: Double): KmaWeatherResponse? {
        val location = bjdPort.locate(lat, lng) ?: return null
        return kmaWeatherPort.getWeather(location)
    }
}
