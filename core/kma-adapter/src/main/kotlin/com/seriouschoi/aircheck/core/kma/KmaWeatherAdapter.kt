package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.KmaWeatherResponse
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * KmaWeatherPort 구현체.
 *
 * 각 예보 종류별 캐시 빈(KmaShortForecastCache 등)을 통해 API를 호출하고
 * KmaWeatherResponse로 조합하여 반환한다.
 * 각 예보 조회 실패 시 null로 처리 — 일부 실패해도 성공한 데이터는 반환한다.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaWeatherAdapter(
    private val shortForecastCache: KmaShortForecastCache,
    private val midLandForecastCache: KmaMidLandForecastCache,
    private val midTaForecastCache: KmaMidTaForecastCache
) : KmaWeatherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getWeather(location: BjdLocation): Result<KmaWeatherResponse> =
        Result.runCatching {
            val short = runCatching { shortForecastCache.fetch(location.lat, location.lng) }
                .onFailure { log.error("단기예보 조회 실패 (lat=${location.lat}, lng=${location.lng})", it) }
                .getOrNull()
            val midLand = runCatching { midLandForecastCache.fetch(location.doCode, location.sigunguCode) }
                .onFailure { log.warn("중기육상예보 조회 실패 (doCode=${location.doCode}, sigunguCode=${location.sigunguCode})", it) }
                .getOrNull()
            val midTa = runCatching { midTaForecastCache.fetch(location.lat, location.lng) }
                .onFailure { log.warn("중기기온예보 조회 실패 (lat=${location.lat}, lng=${location.lng})", it) }
                .getOrNull()
            KmaWeatherResponse(
                shortForecast = short,
                midLandForecast = midLand,
                midTaForecast = midTa
            )
        }
}
