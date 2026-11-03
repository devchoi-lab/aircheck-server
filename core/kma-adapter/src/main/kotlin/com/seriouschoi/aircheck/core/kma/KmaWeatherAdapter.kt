package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.KmaWeatherResponse
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * KmaWeatherPort 구현체.
 *
 * 각 예보 종류별 캐시 빈(KmaShortForecastCache 등)을 통해 API를 호출하고
 * KmaWeatherResponse로 조합하여 반환한다.
 * 중기예보 조회 실패 시 null로 처리 — short forecast 성공이면 전체 실패로 처리하지 않는다.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaWeatherAdapter(
    private val shortForecastCache: KmaShortForecastCache,
    private val midLandForecastCache: KmaMidLandForecastCache,
    private val midTaForecastCache: KmaMidTaForecastCache
) : KmaWeatherPort {

    override fun getWeather(location: BjdLocation): Result<KmaWeatherResponse> =
        Result.runCatching {
            val short = shortForecastCache.fetch(location.lat, location.lng)
            val midLand = runCatching { midLandForecastCache.fetch(location.doCode, location.sigunguCode) }.getOrNull()
            val midTa = runCatching { midTaForecastCache.fetch(location.lat, location.lng) }.getOrNull()
            KmaWeatherResponse(
                shortForecast = short,
                midLandForecast = midLand,
                midTaForecast = midTa
            )
        }
}
