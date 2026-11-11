package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException
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
    /**
     * 법정동을 찾아 기상청 날씨를 조회한다.
     *
     * 법정동을 찾지 못하면 [LocationNotFoundException]으로 실패하고 기상청 API는 호출하지 않는다.
     * 날씨 조회 실패는 포트가 돌려준 실패를 그대로 전달한다.
     */
    fun execute(lat: Double, lng: Double): Result<KmaWeatherResponse> {
        val location = bjdPort.locate(lat, lng)
            ?: return Result.failure(LocationNotFoundException(lat, lng))
        return kmaWeatherPort.getWeather(location)
    }
}
