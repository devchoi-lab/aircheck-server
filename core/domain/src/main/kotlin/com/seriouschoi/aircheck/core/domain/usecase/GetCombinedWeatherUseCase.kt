package com.seriouschoi.aircheck.core.domain.usecase

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.model.DataError
import com.seriouschoi.aircheck.core.domain.model.DataErrorCode
import com.seriouschoi.aircheck.core.domain.model.DataResult
import com.seriouschoi.aircheck.core.domain.model.KmaWeatherResponse
import com.seriouschoi.aircheck.core.domain.model.BjdLocation
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
        val location = bjdPort.locate(lat, lng)

        return CombinedWeatherResult(
            kmaWeather = fetchKmaWeather(location, lat, lng),
            airQuality = fetchAirQuality(location, lat, lng)
        )
    }

    private fun fetchKmaWeather(location: BjdLocation?, lat: Double, lng: Double): DataResult<KmaWeatherResponse> {
        if (location == null) {
            return DataResult.Failure(DataError(
                errorCode = DataErrorCode.LOCATION_NOT_FOUND,
                errorId = log.errorWithId("위치 조회 실패 (lat=$lat, lng=$lng)")
            ))
        }
        val result = kmaWeatherPort.getWeather(location)
        return if (result.isSuccess) {
            DataResult.Success(result.getOrThrow())
        } else {
            DataResult.Failure(DataError(
                errorCode = DataErrorCode.API_UNAVAILABLE,
                errorId = log.errorWithId("KMA 날씨 조회 실패", result.exceptionOrNull())
            ))
        }
    }

    private fun fetchAirQuality(location: BjdLocation?, lat: Double, lng: Double): DataResult<AirQualityResponse> {
        val result = airQualityPort.getAirQuality(location?.lat ?: lat, location?.lng ?: lng)
        return if (result.isSuccess && result.getOrNull() != null) {
            DataResult.Success(result.getOrThrow()!!)
        } else if (result.isFailure) {
            DataResult.Failure(DataError(
                errorCode = DataErrorCode.API_UNAVAILABLE,
                errorId = log.errorWithId("대기질 조회 실패 (lat=$lat, lng=$lng)", result.exceptionOrNull())
            ))
        } else {
            DataResult.Failure(DataError(
                errorCode = DataErrorCode.API_UNAVAILABLE,
                errorId = log.errorWithId("대기질 데이터 없음 (lat=$lat, lng=$lng)")
            ))
        }
    }
}
