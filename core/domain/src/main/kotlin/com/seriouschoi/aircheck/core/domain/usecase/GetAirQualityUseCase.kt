package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException
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
    /**
     * 법정동 대표 좌표로 정규화해서 대기질을 조회한다.
     *
     * 법정동을 찾지 못하면 [LocationNotFoundException]으로 실패하고 대기질 API는 호출하지 않는다.
     * 대기질 조회 실패는 포트가 돌려준 실패를 그대로 전달한다.
     */
    fun execute(lat: Double, lng: Double): Result<AirQualityResponse> {
        val location = bjdPort.locate(lat, lng)
            ?: return Result.failure(LocationNotFoundException(lat, lng))

        return airQualityPort.getAirQuality(location.lat, location.lng)
    }
}
