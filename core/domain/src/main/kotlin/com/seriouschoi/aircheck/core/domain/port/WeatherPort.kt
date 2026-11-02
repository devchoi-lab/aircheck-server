package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.WeatherResponse

/**
 * 날씨 데이터 조회 Port (아웃바운드)
 *
 * 구현체: OpenMeteoAdapter
 *
 * @deprecated KMA 어댑터는 [KmaWeatherPort]로 대체됨.
 * UseCase 전환 완료 후 이 포트와 KmaAdapter는 삭제 예정.
 */
@Deprecated("KmaAdapter는 KmaWeatherPort로 전환. UseCase 전환 후 삭제 예정.")
interface WeatherPort {
    fun getWeather(lat: Double, lng: Double): WeatherResponse?
}
