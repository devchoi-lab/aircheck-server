package com.seriouschoi.aircheck.core.bjd

/**
 * 테스트용 좌표. 운영 코드의 기본 위치와는 별개의 값이다.
 */
internal object TestLocations {
    /** 서울시청. 가장 가까운 법정동 중심은 약 0.2km 거리에 있다. */
    const val SEOUL_CITY_HALL_LAT = 37.5665
    const val SEOUL_CITY_HALL_LNG = 126.9780

    /** 한국에서 멀리 떨어진 좌표 (이름 → 위도, 경도). 실패 메시지에 이름이 나오도록 이름을 붙인다. */
    val FOREIGN_LOCATIONS: Map<String, Pair<Double, Double>> = mapOf(
        "도쿄" to (35.68 to 139.69),
        "뉴욕" to (40.71 to -74.00),
        "(0,0)" to (0.0 to 0.0)
    )
}
