package com.seriouschoi.aircheck.core.domain.model

/**
 * KMA API 3종 조회 결과를 담는 도메인 모델.
 *
 * 각 필드는 독립적인 API 호출 결과이므로 개별적으로 성공/실패한다.
 * 하나가 실패해도 나머지 성공한 데이터는 그대로 사용 가능.
 * 전체를 Failure로 만들면 안 됨 — KmaWeatherAdapter에서 개별 runCatching으로 실패를 흡수하는 이유.
 *
 * - shortForecast: 초단기실황 + 단기예보 (현재날씨·시간별·일별)
 * - midLandForecast: 중기육상예보 (3~10일 강수확률·날씨)
 * - midTaForecast: 중기기온예보 (3~10일 최저·최고기온)
 */
data class KmaWeatherResponse(
    val shortForecast: DataResult<ShortForecast>,
    val midLandForecast: DataResult<MidLandForecast>,
    val midTaForecast: DataResult<MidTaForecast>
)

data class ShortForecast(
    val current: CurrentWeather,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>
)
