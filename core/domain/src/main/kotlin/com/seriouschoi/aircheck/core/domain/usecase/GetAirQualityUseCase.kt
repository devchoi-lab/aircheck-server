package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import jakarta.inject.Inject
import jakarta.inject.Named

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetAirQualityUseCase @Inject constructor(
    private val airQualityPort: AirQualityPort
) {
    fun execute(lat: Double, lng: Double): AirQualityResponse? =
        airQualityPort.getAirQuality(lat, lng)
}
