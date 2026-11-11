package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.ShortForecast

/**
 * 기상청(KMA) 날씨 데이터 조회 Port (아웃바운드)
 *
 * 예보 종류별로 따로 조회한다. 실패 원인은 호출한 쪽(유스케이스)이 보고 판단한다.
 * 구현체: KmaWeatherAdapter
 */
interface KmaWeatherPort {
    /** 초단기실황 + 단기예보 */
    fun getShortForecast(location: BjdLocation): Result<ShortForecast>

    /** 중기육상예보 (3~10일 강수확률·날씨) */
    fun getMidLandForecast(location: BjdLocation): Result<MidLandForecast>

    /** 중기기온예보 (3~10일 최저·최고기온) */
    fun getMidTaForecast(location: BjdLocation): Result<MidTaForecast>
}
