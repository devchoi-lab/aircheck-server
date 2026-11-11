package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException
import com.seriouschoi.aircheck.core.domain.model.CombinedWeather
import com.seriouschoi.aircheck.core.domain.model.DataErrorTarget
import com.seriouschoi.aircheck.core.domain.model.KmaWeather
import com.seriouschoi.aircheck.core.domain.model.toDataError
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import jakarta.inject.Inject
import jakarta.inject.Named
import org.slf4j.LoggerFactory

/** DI 컨테이너 등록용 (@Component 역할, jakarta.inject 표준) */
@Named
class GetCombinedWeatherUseCase @Inject constructor(
    private val bjdPort: BjdPort,
    private val kmaWeatherPort: KmaWeatherPort,
    private val airQualityPort: AirQualityPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 법정동을 찾지 못하면 어떤 API도 호출하지 않고 [LocationNotFoundException]으로 실패한다.
     * 법정동을 찾은 뒤에는 항목별 실패를 [CombinedWeather.errors]에 담고 나머지 항목은 그대로 돌려준다.
     * 모든 항목이 실패해도 성공(빈 항목 + 에러 목록)으로 돌려준다.
     */
    fun execute(lat: Double, lng: Double): Result<CombinedWeather> {
        // 법정동을 찾지 못하면 외부 API를 호출하지 않는다 (원본 좌표로 호출하면 캐시 키가 좌표 단위로 흩어진다)
        val location = bjdPort.locate(lat, lng)
            ?: return Result.failure(LocationNotFoundException(lat, lng))

        val short = kmaWeatherPort.getShortForecast(location)
        val midLand = kmaWeatherPort.getMidLandForecast(location)
        val midTa = kmaWeatherPort.getMidTaForecast(location)
        val air = airQualityPort.getAirQuality(location.lat, location.lng)

        // 실패한 항목은 null로 두고, 원인을 로그로 남긴 뒤 에러 목록에 담는다.
        val errors = listOfNotNull(
            short.toDataError(DataErrorTarget.SHORT_FORECAST, log, "단기예보 조회 실패 (lat=$lat, lng=$lng)"),
            midLand.toDataError(DataErrorTarget.MID_LAND_FORECAST, log, "중기육상예보 조회 실패 (lat=$lat, lng=$lng)"),
            midTa.toDataError(DataErrorTarget.MID_TA_FORECAST, log, "중기기온예보 조회 실패 (lat=$lat, lng=$lng)"),
            air.toDataError(DataErrorTarget.AIR_QUALITY, log, "대기질 조회 실패 (lat=$lat, lng=$lng)")
        )

        return Result.success(
            CombinedWeather(
                kmaWeather = KmaWeather(
                    shortForecast = short.getOrNull(),
                    midLandForecast = midLand.getOrNull(),
                    midTaForecast = midTa.getOrNull()
                ),
                airQuality = air.getOrNull(),
                errors = errors
            )
        )
    }
}
