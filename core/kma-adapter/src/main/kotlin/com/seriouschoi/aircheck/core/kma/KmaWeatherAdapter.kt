package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.DataError
import com.seriouschoi.aircheck.core.domain.model.DataErrorCode
import com.seriouschoi.aircheck.core.domain.model.DataResult
import com.seriouschoi.aircheck.core.domain.model.KmaWeatherResponse
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import com.seriouschoi.aircheck.util.logging.errorWithId
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * KmaWeatherPort 구현체.
 *
 * 각 예보 종류별 캐시 빈(KmaShortForecastCache 등)을 통해 API를 호출하고
 * KmaWeatherResponse로 조합하여 반환한다.
 * 각 예보 조회 실패 시 DataResult.Failure — 일부 실패해도 성공한 데이터는 반환한다.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaWeatherAdapter(
    private val shortForecastCache: KmaShortForecastCache,
    private val midLandForecastCache: KmaMidLandForecastCache,
    private val midTaForecastCache: KmaMidTaForecastCache
) : KmaWeatherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getWeather(location: BjdLocation): KmaWeatherResponse {
        val short = runCatching { shortForecastCache.fetch(location.lat, location.lng) }
            .fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(DataError(
                    DataErrorCode.API_UNAVAILABLE,
                    log.errorWithId("단기예보 조회 실패 (lat=${location.lat}, lng=${location.lng})", it)
                )) }
            )
        val midLand = runCatching { midLandForecastCache.fetch(location.doCode, location.sigunguCode) }
            .fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(DataError(
                    DataErrorCode.API_UNAVAILABLE,
                    log.errorWithId("중기육상예보 조회 실패 (doCode=${location.doCode}, sigunguCode=${location.sigunguCode})", it)
                )) }
            )
        val midTa = runCatching { midTaForecastCache.fetch(location.lat, location.lng) }
            .fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(DataError(
                    DataErrorCode.API_UNAVAILABLE,
                    log.errorWithId("중기기온예보 조회 실패 (lat=${location.lat}, lng=${location.lng})", it)
                )) }
            )
        return KmaWeatherResponse(
            shortForecast = short,
            midLandForecast = midLand,
            midTaForecast = midTa
        )
    }
}
