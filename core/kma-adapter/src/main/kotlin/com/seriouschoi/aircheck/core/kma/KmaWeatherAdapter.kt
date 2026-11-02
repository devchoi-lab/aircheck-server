package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * KmaWeatherPort 구현체.
 *
 * 각 예보 종류별 캐시 빈(KmaShortForecastCache 등)을 통해 API를 호출하고
 * 결과를 Result로 감싸서 반환한다.
 * UseCase에서 실패 원인에 따른 분기 처리가 가능하다.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaWeatherAdapter(
    private val shortForecastCache: KmaShortForecastCache,
    private val midLandForecastCache: KmaMidLandForecastCache,
    private val midTaForecastCache: KmaMidTaForecastCache
) : KmaWeatherPort {

    override fun getShortForecast(lat: Double, lng: Double): Result<WeatherResponse> =
        Result.runCatching { shortForecastCache.fetch(lat, lng) }

    override fun getMidLandForecast(doCode: Long): Result<MidLandForecast> =
        Result.runCatching { midLandForecastCache.fetch(doCode) }

    override fun getMidTaForecast(lat: Double, lng: Double): Result<MidTaForecast> =
        Result.runCatching { midTaForecastCache.fetch(lat, lng) }
}
