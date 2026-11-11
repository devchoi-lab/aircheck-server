package com.seriouschoi.aircheck.core.domain.model

import java.time.LocalDate

/**
 * 중기육상예보 (3~10일, getMidLandFcst API 응답)
 *
 * doCode → landRegId 매핑 후 조회. KmaWeather에 포함되어 반환.
 */
data class MidLandForecast(
    val days: List<MidLandForecastDay>
)

/**
 * @param date          예보 날짜 (KST 기준). 발표일 + 3~10일
 * @param amPrecipProb  오전 강수확률 (%). 8~10일은 단일값을 am/pm 공통 사용. 값이 없으면 null
 * @param pmPrecipProb  오후 강수확률 (%). 값이 없으면 null
 * @param amWeather     오전 날씨. 기상청 문구와 타입을 함께 담는다. 값을 가져오지 못하면 null
 * @param pmWeather     오후 날씨. 값을 가져오지 못하면 null
 */
data class MidLandForecastDay(
    val date: LocalDate,
    val amPrecipProb: Int?,
    val pmPrecipProb: Int?,
    val amWeather: MidLandWeather?,
    val pmWeather: MidLandWeather?
)

/**
 * 중기육상예보 날씨 (기상청 wf* 필드).
 *
 * 기상청은 "맑음", "흐리고 비" 같은 문구 하나로 하늘상태와 강수를 함께 내려준다.
 * 단기예보의 SKY/PTY 코드와는 다른 체계이고 중기예보에만 값이 추가될 수 있어서, 단기 타입을 재사용하지 않고 따로 둔다.
 *
 * @param type 문구에 해당하는 타입. 기상청 문서에 없는 문구이면 [MidLandWeatherType.UNKNOWN]
 * @param text 기상청이 내려준 원문. 타입이 정해져도 항상 원문 그대로 담는다
 */
data class MidLandWeather(
    val type: MidLandWeatherType,
    val text: String
)

/**
 * 중기육상예보 날씨 문구. 기상청 문서(하늘상태) 기준.
 */
enum class MidLandWeatherType(val kmaText: String?) {
    CLEAR("맑음"),
    MOSTLY_CLOUDY("구름많음"),
    MOSTLY_CLOUDY_RAIN("구름많고 비"),
    MOSTLY_CLOUDY_SNOW("구름많고 눈"),
    MOSTLY_CLOUDY_RAIN_SNOW("구름많고 비/눈"),
    MOSTLY_CLOUDY_SHOWER("구름많고 소나기"),
    OVERCAST("흐림"),
    OVERCAST_RAIN("흐리고 비"),
    OVERCAST_SNOW("흐리고 눈"),
    OVERCAST_RAIN_SNOW("흐리고 비/눈"),
    OVERCAST_SHOWER("흐리고 소나기"),

    /** 값은 왔지만 기상청 문서에 없는 문구 (기상청이 값을 추가했을 때). 값이 없는 것(null)과 구분된다. */
    UNKNOWN(null);

    companion object {
        /** 문구가 일치하는 타입. 일치하는 것이 없으면 [UNKNOWN]. */
        fun fromKmaText(text: String): MidLandWeatherType = entries.firstOrNull { it.kmaText == text } ?: UNKNOWN
    }
}
