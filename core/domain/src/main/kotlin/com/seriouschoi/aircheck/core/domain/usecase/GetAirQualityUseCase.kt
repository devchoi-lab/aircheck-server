package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import jakarta.inject.Inject
import jakarta.inject.Named

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetAirQualityUseCase @Inject constructor(
    private val bjdPort: BjdPort,
    private val airQualityPort: AirQualityPort
) {
    fun execute(lat: Double, lng: Double): AirQualityResponse? {
        val location = bjdPort.locate(lat, lng)
        val normLat = location?.lat ?: lat
        val normLng = location?.lng ?: lng
        return airQualityPort.getAirQuality(normLat, normLng)
    }
}
