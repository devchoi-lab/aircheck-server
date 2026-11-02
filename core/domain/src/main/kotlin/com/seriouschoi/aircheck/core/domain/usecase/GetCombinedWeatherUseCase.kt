package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.model.assembleMidTermForecast
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import jakarta.inject.Inject
import jakarta.inject.Named
import java.util.Optional

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetCombinedWeatherUseCase @Inject constructor(
    private val bjdPort: BjdPort,
    @Suppress("DEPRECATION") private val weatherPort: WeatherPort,
    private val airQualityPort: AirQualityPort,
    private val kmaWeatherPort: Optional<KmaWeatherPort>
) {
    fun execute(lat: Double, lng: Double): CombinedWeatherResult {
        val location = bjdPort.locate(lat, lng)
        val normLat = location?.lat ?: lat
        val normLng = location?.lng ?: lng

        val kma = kmaWeatherPort.orElse(null)
        val weather = if (kma != null && location != null) {
            val short = kma.getShortForecast(normLat, normLng).getOrNull()
            if (short != null) {
                val midLand = kma.getMidLandForecast(location.doCode).getOrNull()
                val midTa = kma.getMidTaForecast(normLat, normLng).getOrNull()
                val midTerm = if (midLand != null && midTa != null) assembleMidTermForecast(midLand, midTa) else emptyList()
                short.copy(midTermForecast = midTerm)
            } else null
        } else null

        @Suppress("DEPRECATION")
        val resolvedWeather = weather ?: weatherPort.getWeather(normLat, normLng)

        return CombinedWeatherResult(
            weather = resolvedWeather,
            airQuality = airQualityPort.getAirQuality(normLat, normLng)
        )
    }
}
