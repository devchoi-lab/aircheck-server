package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.KmaWeatherResponse

/**
 * 기상청(KMA) 날씨 데이터 조회 Port (아웃바운드)
 *
 * 구현체: KmaWeatherAdapter
 */
interface KmaWeatherPort {
    /**
     * 초단기실황 + 단기예보 + 중기예보를 조합하여 반환. 실패 시 Result.failure.
     *
     * 중기예보(midLandForecast, midTaForecast)는 조회 실패 시 null로 반환됨.
     */
    fun getWeather(location: BjdLocation): Result<KmaWeatherResponse>
}
