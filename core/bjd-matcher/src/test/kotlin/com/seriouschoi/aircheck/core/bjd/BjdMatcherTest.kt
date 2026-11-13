package com.seriouschoi.aircheck.core.bjd

import com.seriouschoi.aircheck.core.bjd.TestLocations.SEOUL_CITY_HALL_LAT
import com.seriouschoi.aircheck.core.bjd.TestLocations.SEOUL_CITY_HALL_LNG
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BjdMatcherTest {

    @Test
    fun `가장 가까운 법정동이 최대 거리 안이면, 그 법정동을 리턴한다`() {
        val matched = BjdMatcher.match(SEOUL_CITY_HALL_LAT, SEOUL_CITY_HALL_LNG, maxDistanceKm = 5.0)

        assertNotNull(matched, "5km 안에 법정동이 있어야 합니다.")
    }

    @Test
    fun `가장 가까운 법정동이 최대 거리 밖이면, null을 리턴한다`() {
        // 서울시청에서 가장 가까운 법정동 중심은 약 0.2km 거리라 10m 한도를 넘는다.
        val matched = BjdMatcher.match(SEOUL_CITY_HALL_LAT, SEOUL_CITY_HALL_LNG, maxDistanceKm = 0.01)

        assertNull(matched, "최대 거리가 10m인데 ${matched?.code}를 리턴했습니다.")
    }
}
