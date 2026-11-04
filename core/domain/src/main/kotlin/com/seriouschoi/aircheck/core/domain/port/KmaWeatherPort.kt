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
     * 초단기실황 + 단기예보 + 중기예보를 조합하여 반환.
     * 각 예보는 개별 DataResult로 성공/실패를 가지며, 하나 실패해도 나머지는 유효하다.
     */
    fun getWeather(location: BjdLocation): KmaWeatherResponse
}
