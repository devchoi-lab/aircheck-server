package com.seriouschoi.aircheck.core.domain.model

/**
 * KMA API 3종 조회 결과를 담는 도메인 모델
 *
 * - shortForecast: 초단기실황 + 단기예보 (현재날씨·시간별·일별)
 * - midLandForecast: 중기육상예보 (3~10일 강수확률·날씨). 조회 실패 시 null.
 * - midTaForecast: 중기기온예보 (3~10일 최저·최고기온). 조회 실패 시 null.
 */
data class KmaWeather(
    val shortForecast: ShortForecast?,
    val midLandForecast: MidLandForecast?,
    val midTaForecast: MidTaForecast?
)
