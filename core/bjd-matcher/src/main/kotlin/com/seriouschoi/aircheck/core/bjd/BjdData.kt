package com.seriouschoi.aircheck.core.bjd

/**
 * 법정동(法定洞) 데이터
 *
 * 출처: 국토교통부_전국 법정동 공공데이터
 *
 * @param code     법정동 코드
 * @param doGroup  시도 (항상 존재)
 * @param guGroup  시군구 (없을 수 있음)
 * @param dongGroup 읍면동 (없을 수 있음)
 * @param latLng   중심 좌표. null이면 말단 단위(읍면동) 아님 — BjdMatcher 매칭 대상 아님
 */
data class BjdData(
    val code: String,
    val doGroup: BjdGroup,
    val guGroup: BjdGroup?,
    val dongGroup: BjdGroup?,
    val latLng: Pair<Double, Double>?
)

data class BjdGroup(
    val name: String,
    val code: Long
)
