package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse

/**
 * 기상청(KMA) 날씨 데이터 조회 Port (아웃바운드)
 *
 * WeatherPort와 달리 예보 종류별로 메서드가 분리되어 있어
 * UseCase에서 doCode를 활용한 중기육상예보 지역 매핑이 가능하다.
 *
 * 구현체: KmaWeatherAdapter
 */
interface KmaWeatherPort {
    /** 초단기실황 + 단기예보 (midTermForecast는 빈 리스트). 실패 시 Result.failure. */
    fun getShortForecast(lat: Double, lng: Double): Result<WeatherResponse>

    /**
     * 중기육상예보 (3~10일 강수확률·날씨). 실패 시 Result.failure.
     *
     * @param doCode 법정동 도 단위 코드 (BjdLocation.doCode). 내부에서 landRegId로 변환.
     * 강원도(5100000000)는 영서/영동 구분 불가 — 영서(11D10000)로 기본 매핑됨.
     */
    fun getMidLandForecast(doCode: Long): Result<MidLandForecast>

    /** 중기기온예보 (3~10일 최저·최고기온). lat/lng → taRegId 변환 후 조회. 실패 시 Result.failure. */
    fun getMidTaForecast(lat: Double, lng: Double): Result<MidTaForecast>
}
