package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.ShortForecast
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * KmaWeatherPort 구현체.
 *
 * 각 예보 종류별 캐시 빈(KmaShortForecastCache 등)을 통해 API를 호출한다.
 * 실패는 삼키지 않고 Result.failure로 돌려준다.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaWeatherAdapter(
    private val shortForecastCache: KmaShortForecastCache,
    private val midLandForecastCache: KmaMidLandForecastCache,
    private val midTaForecastCache: KmaMidTaForecastCache
) : KmaWeatherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getShortForecast(location: BjdLocation): Result<ShortForecast> =
        runCatching { shortForecastCache.fetch(location.lat, location.lng) }
            .onFailure { log.error("단기예보 조회 실패 (lat=${location.lat}, lng=${location.lng})", it) }

    override fun getMidLandForecast(location: BjdLocation): Result<MidLandForecast> =
        runCatching { midLandForecastCache.fetch(location.doCode, location.sigunguCode) }
            .onFailure { log.warn("중기육상예보 조회 실패 (doCode=${location.doCode}, sigunguCode=${location.sigunguCode})", it) }

    override fun getMidTaForecast(location: BjdLocation): Result<MidTaForecast> =
        runCatching { midTaForecastCache.fetch(location.lat, location.lng) }
            .onFailure { log.warn("중기기온예보 조회 실패 (lat=${location.lat}, lng=${location.lng})", it) }
}
