package com.seriouschoi.aircheck.core.airkorea

import com.seriouschoi.aircheck.core.airkorea.cache.AirQualityCache
import com.seriouschoi.aircheck.core.domain.exception.AirQualityNotFoundException
import com.seriouschoi.aircheck.core.domain.model.AirQuality
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// ── Adapter 구현 ───────────────────────────────────────────────────────────

@Component
class AirKoreaAdapter(
    private val airQualityCache: AirQualityCache
) : AirQualityPort {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * lat, lng를 기반으로, 공기질 정보를 가져온다. (캐시 적용: [AirQualityCache])
     *
     * lat, lng기반으로 가까운 측정소 정보를 가져와서,
     * 해당 측정소의 공기질 측정 값을 리턴한다.
     */
    override fun getAirQuality(lat: Double, lng: Double): Result<AirQuality> =
        Result.runCatching { airQualityCache.fetch(lat, lng) }
            .onFailure {
                // 데이터가 없는 것은 장애가 아니므로 에러 로그를 남기지 않는다
                if (it is AirQualityNotFoundException) log.warn(it.message)
                else log.error("대기질 조회 실패 (lat=$lat, lng=$lng)", it)
            }
}
