package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.model.DataError
import com.seriouschoi.aircheck.core.domain.model.DataErrorCode
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import com.seriouschoi.aircheck.util.logging.errorWithId
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

    fun execute(lat: Double, lng: Double): CombinedWeatherResult {
        val errors = mutableListOf<DataError>()
        val location = bjdPort.locate(lat, lng)

        if (location == null) {
            errors.add(DataError(
                errorCode = DataErrorCode.LOCATION_NOT_FOUND,
                errorId = log.errorWithId("위치 조회 실패 (lat=$lat, lng=$lng)")
            ))
        }

        val kmaWeather = location?.let { kmaWeatherPort.getWeather(it).getOrNull() }
        if (kmaWeather?.shortForecast == null) {
            errors.add(DataError(
                errorCode = DataErrorCode.API_UNAVAILABLE,
                errorId = log.errorWithId("KMA 단기예보 조회 실패")
            ))
        }
        if (kmaWeather?.midTaForecast == null) {
            errors.add(DataError(
                errorCode = DataErrorCode.API_UNAVAILABLE,
                errorId = log.errorWithId("KMA 중기기온 조회 실패")
            ))
        }
        if (kmaWeather?.midLandForecast == null) {
            errors.add(DataError(
                errorCode = DataErrorCode.API_UNAVAILABLE,
                errorId = log.errorWithId("KMA 중기육상예보 조회 실패")
            ))
        }

        // 법정동을 찾지 못하면 대기질은 호출하지 않는다 (원본 좌표로 호출하면 캐시 키가 좌표 단위로 흩어진다)
        val airResult = location?.let { airQualityPort.getAirQuality(it.lat, it.lng) }
        val airQuality = airResult?.getOrNull()
        if (airQuality == null) {
            errors.add(DataError(
                errorCode = DataErrorCode.API_UNAVAILABLE,
                errorId = log.errorWithId("대기질 조회 실패 (lat=$lat, lng=$lng)", airResult?.exceptionOrNull())
            ))
        }

        return CombinedWeatherResult(
            kmaWeather = kmaWeather,
            airQuality = airQuality,
            errors = errors
        )
    }
}
