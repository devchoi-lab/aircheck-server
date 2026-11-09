package com.seriouschoi.aircheck.core.domain.model

/**
 * 기상청 강수형태(PTY) 코드. 기상청이 정의한 값을 그대로 담는다.
 *
 * 초단기실황은 0~7, 단기예보는 0~4만 내려온다.
 */
enum class PrecipitationType(val code: Int?) {
    /** 없음 */
    NONE(0),

    /** 비 */
    RAIN(1),

    /** 비/눈 */
    RAIN_SNOW(2),

    /** 눈 */
    SNOW(3),

    /** 소나기 */
    SHOWER(4),

    /** 빗방울 (초단기) */
    RAINDROP(5),

    /** 빗방울눈날림 (초단기) */
    RAINDROP_SNOW_FLURRY(6),

    /** 눈날림 (초단기) */
    SNOW_FLURRY(7),

    /** 값은 왔지만 기상청 문서에 없는 코드 (기상청이 값을 추가했을 때). 값이 없는 것(null)과 구분된다. */
    UNKNOWN(null);

    companion object {
        /** 기상청이 정의하지 않은 코드이면 [UNKNOWN]. */
        fun fromCode(code: Int): PrecipitationType = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/**
 * 기상청 하늘상태(SKY) 코드. 단기예보에서만 내려온다 (초단기실황에는 없음).
 */
enum class SkyCondition(val code: Int?) {
    /** 맑음 */
    CLEAR(1),

    /** 구름많음 */
    MOSTLY_CLOUDY(3),

    /** 흐림 */
    OVERCAST(4),

    /** 값은 왔지만 기상청 문서에 없는 코드 (기상청이 값을 추가했을 때). 값이 없는 것(null)과 구분된다. */
    UNKNOWN(null);

    companion object {
        /** 기상청이 정의하지 않은 코드이면 [UNKNOWN]. */
        fun fromCode(code: Int): SkyCondition = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}
